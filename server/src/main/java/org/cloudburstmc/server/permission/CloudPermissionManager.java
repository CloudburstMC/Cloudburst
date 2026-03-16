package org.cloudburstmc.server.permission;

import com.google.inject.Singleton;
import org.cloudburstmc.api.permission.Permissible;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionDefault;
import org.cloudburstmc.api.permission.PermissionManager;

import java.util.*;

/**
 * Server-side {@link PermissionManager} implementation.
 *
 * <p>Permissions are indexed in two default sets ({@code op} and {@code non-op}) and tracked via
 * weak-reference subscription maps so that offline or GC'd permissibles are cleaned up
 * automatically.</p>
 *
 * <p>Subscriber maps use {@code WeakHashMap<Permissible, Boolean>} with value {@code Boolean.TRUE}
 * as a weak set. This is the standard Java idiom: the value holds no reference to the key,
 * so GC can evict entries for unreachable permissibles.</p>
 *
 * <p>This class is NOT thread-safe. All access must occur on the server's primary thread.</p>
 */
@Singleton
public class CloudPermissionManager implements PermissionManager {

    private final Map<String, Permission> permissions = new HashMap<>();

    /**
     * Permissions granted to non-operators by default.
     */
    private final Map<String, Permission> defaultPerms = new HashMap<>();

    /**
     * Permissions granted to operators by default.
     */
    private final Map<String, Permission> defaultPermsOp = new HashMap<>();

    /**
     * Per-permission subscriber index. Inner map is a weak set keyed on {@link Permissible}.
     */
    private final Map<String, WeakHashMap<Permissible, Boolean>> permSubs = new HashMap<>();

    /**
     * Default-set subscribers keyed by op status. {@code true} = operator subscribers,
     * {@code false} = non-operator subscribers. Both inner maps are weak sets.
     * The outer map is immutable (both keys pre-initialized); only the inner maps are mutated.
     */
    private final Map<Boolean, WeakHashMap<Permissible, Boolean>> defSubs = Map.of(
            Boolean.TRUE, new WeakHashMap<>(),
            Boolean.FALSE, new WeakHashMap<>()
    );

    @Override
    public Optional<Permission> getPermission(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        return Optional.ofNullable(permissions.get(name.toLowerCase(Locale.ROOT)));
    }

    @Override
    public void addPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }

        String key = permission.getName().toLowerCase(Locale.ROOT);
        if (permissions.containsKey(key)) {
            throw new IllegalArgumentException("The permission " + key + " is already defined!");
        }

        permissions.put(key, permission);
        calculatePermissionDefault(permission, true);
    }

    @Override
    public void removePermission(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        String key = name.toLowerCase(Locale.ROOT);

        boolean wasOp = defaultPermsOp.remove(key) != null;
        boolean wasNonOp = defaultPerms.remove(key) != null;

        permissions.remove(key);

        Set<Permissible> affected = new HashSet<>();

        WeakHashMap<Permissible, Boolean> directSubs = permSubs.remove(key);
        if (directSubs != null) {
            affected.addAll(directSubs.keySet());
        }

        if (wasOp) {
            affected.addAll(getDefaultPermSubscriptions(true));
        }

        if (wasNonOp) {
            affected.addAll(getDefaultPermSubscriptions(false));
        }

        for (Permissible p : affected) {
            p.recalculatePermissions();
        }
    }

    @Override
    public void removePermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }
        removePermission(permission.getName());
    }

    /**
     * Returns a snapshot copy of permissions that are granted by default for the given
     * operator status.
     *
     * @param op {@code true} to query the operator defaults, {@code false} for non-operator defaults
     * @return a mutable snapshot; modifications have no effect on the manager
     */
    @Override
    public Map<String, Permission> getDefaultPermissions(boolean op) {
        return new HashMap<>(op ? defaultPermsOp : defaultPerms);
    }

    @Override
    public void recalculatePermissionDefaults(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }

        String key = permission.getName().toLowerCase(Locale.ROOT);
        if (!permissions.containsKey(key)) {
            return;
        }

        boolean wasOp = defaultPermsOp.remove(key) != null;
        boolean wasNonOp = defaultPerms.remove(key) != null;

        calculatePermissionDefault(permission, false);

        boolean nowOp = defaultPermsOp.containsKey(key);
        boolean nowNonOp = defaultPerms.containsKey(key);

        if (wasOp || nowOp) {
            dirtyPermissibles(true);
        }

        if (wasNonOp || nowNonOp) {
            dirtyPermissibles(false);
        }
    }

    /**
     * Adds the permission to the appropriate default sets and optionally notifies subscribers.
     *
     * @param permission the permission to categorize
     * @param dirty      if {@code true}, calls {@link #dirtyPermissibles} after inserting into a set
     */
    private void calculatePermissionDefault(Permission permission, boolean dirty) {
        String key = permission.getName().toLowerCase(Locale.ROOT);
        PermissionDefault def = permission.getDefault();
        if (def == PermissionDefault.OP || def == PermissionDefault.TRUE) {
            defaultPermsOp.put(key, permission);
            if (dirty) {
                dirtyPermissibles(true);
            }
        }

        if (def == PermissionDefault.NOT_OP || def == PermissionDefault.TRUE) {
            defaultPerms.put(key, permission);
            if (dirty) {
                dirtyPermissibles(false);
            }
        }
    }

    private void dirtyPermissibles(boolean op) {
        for (Permissible p : getDefaultPermSubscriptions(op)) {
            p.recalculatePermissions();
        }
    }

    @Override
    public void subscribeToPermission(String permission, Permissible permissible) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }

        if (permissible == null) {
            throw new IllegalArgumentException("Permissible cannot be null");
        }

        permSubs.computeIfAbsent(permission.toLowerCase(Locale.ROOT), k -> new WeakHashMap<>())
                .put(permissible, Boolean.TRUE);
    }

    @Override
    public void unsubscribeFromPermission(String permission, Permissible permissible) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }

        if (permissible == null) {
            throw new IllegalArgumentException("Permissible cannot be null");
        }

        String key = permission.toLowerCase(Locale.ROOT);
        WeakHashMap<Permissible, Boolean> subs = permSubs.get(key);
        if (subs == null) {
            return;
        }

        subs.remove(permissible);
        if (subs.isEmpty()) {
            permSubs.remove(key);
        }
    }

    @Override
    public Set<Permissible> getPermissionSubscriptions(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }

        WeakHashMap<Permissible, Boolean> subs = permSubs.get(permission.toLowerCase(Locale.ROOT));
        if (subs == null) {
            return Set.of();
        }

        return new HashSet<>(subs.keySet());
    }

    @Override
    public void subscribeToDefaultPerms(boolean op, Permissible permissible) {
        if (permissible == null) {
            throw new IllegalArgumentException("Permissible cannot be null");
        }
        defSubs.get(op).put(permissible, Boolean.TRUE);
    }

    @Override
    public void unsubscribeFromDefaultPerms(boolean op, Permissible permissible) {
        if (permissible == null) {
            throw new IllegalArgumentException("Permissible cannot be null");
        }
        defSubs.get(op).remove(permissible);
    }

    @Override
    public Set<Permissible> getDefaultPermSubscriptions(boolean op) {
        return new HashSet<>(defSubs.get(op).keySet());
    }

    @Override
    public Map<String, Permission> getPermissions() {
        return new HashMap<>(permissions);
    }
}
