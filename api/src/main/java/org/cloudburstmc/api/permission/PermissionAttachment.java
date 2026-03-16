package org.cloudburstmc.api.permission;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.*;

/**
 * Holds a set of permission grants scoped to a single plugin and a single {@link Permissible}.
 *
 * <p>Every mutation method triggers {@link Permissible#recalculatePermissions()} on the owning
 * permissible immediately. Use the bulk methods ({@link #setPermissions}/{@link #unsetPermissions})
 * when applying multiple changes to avoid redundant recalculations.</p>
 *
 * <p><strong>All permission names are normalized to lowercase</strong> before storage, so
 * {@link #getPermissions()} will always return lowercase keys regardless of the case used when
 * calling {@link #setPermission(String, boolean)} or {@link #setPermissions(java.util.Map)}.</p>
 */
public final class PermissionAttachment {

    private final LinkedHashMap<String, Boolean> permissions = new LinkedHashMap<>();
    private final Permissible permissible;
    private final PluginContainer plugin;
    private @Nullable PermissionRemovedExecutor removalCallback;

    /**
     * Creates a new attachment owned by {@code plugin} and associated with {@code permissible}.
     *
     * <p>Callers should not invoke this constructor directly; use
     * {@link Permissible#addAttachment(PluginContainer)} instead. The plugin-liveness check
     * is enforced by {@code PermissibleBase.addAttachmentInternal()} before this constructor
     * is called.</p>
     */
    public PermissionAttachment(PluginContainer plugin, Permissible permissible) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (permissible == null) {
            throw new IllegalArgumentException("Permissible cannot be null");
        }
        this.plugin = plugin;
        this.permissible = permissible;
    }

    /**
     * Returns the plugin that owns this attachment.
     */
    public PluginContainer getPlugin() {
        return plugin;
    }

    /**
     * Returns the {@link Permissible} this attachment belongs to.
     */
    public Permissible getPermissible() {
        return permissible;
    }

    /**
     * Returns the configured removal callback, or {@code null} if not set.
     */
    public @Nullable PermissionRemovedExecutor getRemovalCallback() {
        return removalCallback;
    }

    /**
     * Sets the callback invoked when this attachment is removed via {@link #remove()}.
     */
    public void setRemovalCallback(@Nullable PermissionRemovedExecutor executor) {
        this.removalCallback = executor;
    }

    /**
     * Returns a snapshot copy of the current permissions map.
     *
     * <p>The returned map reflects the state at call time; modifications to this attachment
     * afterward are not reflected in it.</p>
     */
    public Map<String, Boolean> getPermissions() {
        return new LinkedHashMap<>(permissions);
    }

    /**
     * Applies all entries from the given map to this attachment and triggers a single recalculation.
     * Keys are normalized to lowercase. Null keys or null values are rejected with
     * {@link IllegalArgumentException}.
     *
     * <p>The entire map is validated before any mutation is applied, so a failed validation
     * leaves the attachment in its previous state.</p>
     */
    public void setPermissions(Map<String, Boolean> permissions) {
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions map cannot be null");
        }

        LinkedHashMap<String, Boolean> normalized = new LinkedHashMap<>(permissions.size());
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("Permissions map must not contain null keys");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("Permissions map must not contain null values");
            }
            normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }

        this.permissions.putAll(normalized);
        permissible.recalculatePermissions();
    }

    /**
     * Removes all permissions from this attachment and triggers recalculation.
     */
    public void clearPermissions() {
        permissions.clear();
        permissible.recalculatePermissions();
    }

    /**
     * Removes all named entries from this attachment and triggers a single recalculation.
     * Names are normalized to lowercase to match storage.
     *
     * <p>All entries are validated before any mutation is applied, so a null element in the
     * iterable throws {@link IllegalArgumentException} without modifying the attachment.</p>
     */
    public void unsetPermissions(Iterable<String> permissions) {
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions iterable cannot be null");
        }

        List<String> normalized = new ArrayList<>();
        for (String node : permissions) {
            if (node == null) {
                throw new IllegalArgumentException("Permission name in iterable cannot be null");
            }
            normalized.add(node.toLowerCase(Locale.ROOT));
        }

        for (String key : normalized) {
            this.permissions.remove(key);
        }

        permissible.recalculatePermissions();
    }

    /**
     * Sets the given permission to the specified value and triggers recalculation.
     */
    public void setPermission(Permission permission, boolean value) {
        setPermission(permission.getName(), value);
    }

    /**
     * Sets the named permission to the specified value and triggers recalculation.
     * The name is normalized to lowercase so that lookups and removals are case-insensitive.
     *
     * <p>Always triggers recalculation even if the permission was already set to the same value,
     * to keep the behavior simple and predictable. Use the bulk {@link #setPermissions(java.util.Map)}
     * method when applying many changes at once to avoid redundant recalculations.</p>
     */
    public void setPermission(String name, boolean value) {
        permissions.put(name.toLowerCase(Locale.ROOT), value);
        permissible.recalculatePermissions();
    }

    /**
     * Removes the given permission from this attachment and triggers recalculation.
     */
    public void unsetPermission(Permission permission) {
        unsetPermission(permission.getName());
    }

    /**
     * Removes the named permission from this attachment and triggers recalculation.
     * The name is normalized to lowercase to match storage.
     *
     * <p>Always triggers recalculation even if no entry was present, to keep the behavior
     * simple and predictable.</p>
     */
    public void unsetPermission(String name) {
        permissions.remove(name.toLowerCase(Locale.ROOT));
        permissible.recalculatePermissions();
    }

    /**
     * Removes this attachment from its owning {@link Permissible}.
     *
     * @return {@code true} if successfully removed; {@code false} if it was no longer attached
     */
    public boolean remove() {
        try {
            permissible.removeAttachment(this);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
