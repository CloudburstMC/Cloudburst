package org.cloudburstmc.api.permission;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Manages permission registration, default resolution, and subscription tracking.
 *
 * <p>The subscription model allows {@link Permissible}s to be notified when permissions
 * they hold are changed so their effective permission cache is kept current.</p>
 */
public interface PermissionManager {

    /**
     * Returns the registered permission with the given name, or an empty optional.
     */
    Optional<Permission> getPermission(String name);

    /**
     * Returns {@code true} if a permission with the given name is registered.
     */
    default boolean containsPermission(String name) {
        return getPermission(name).isPresent();
    }

    /**
     * Registers a permission.
     *
     * @throws IllegalArgumentException if a permission with the same name is already registered
     */
    void addPermission(Permission permission);

    /**
     * Registers a permission if no permission with the same name is already registered.
     *
     * <p>Note: this default implementation is not atomic (check-then-act). Custom implementations
     * that allow concurrent access must override this method to provide an atomic check-and-insert.</p>
     *
     * @return {@code true} if the permission was registered; {@code false} if it was already present
     */
    default boolean addPermissionIfAbsent(Permission permission) {
        if (containsPermission(permission.getName())) {
            return false;
        }
        addPermission(permission);
        return true;
    }

    /**
     * Unregisters the permission with the given name.
     *
     * <p>This method also removes the permission from all default sets and triggers
     * {@link Permissible#recalculatePermissions()} on all subscribed permissibles so their
     * effective permission caches are immediately consistent after removal.</p>
     */
    void removePermission(String name);

    /**
     * Unregisters the given permission.
     *
     * <p>This method also removes the permission from all default sets and triggers
     * {@link Permissible#recalculatePermissions()} on all subscribed permissibles so their
     * effective permission caches are immediately consistent after removal.</p>
     */
    void removePermission(Permission permission);

    /**
     * Returns a snapshot copy of permissions that are granted by default for the given
     * operator status.
     *
     * @param op {@code true} to query the operator defaults, {@code false} for non-operator defaults
     */
    Map<String, Permission> getDefaultPermissions(boolean op);

    /**
     * Recalculates which default sets this permission belongs to and notifies subscribers.
     *
     * <p>Called automatically when a permission's default value changes.</p>
     */
    void recalculatePermissionDefaults(Permission permission);

    /**
     * Subscribes a {@link Permissible} to receive notifications when the named permission changes.
     */
    void subscribeToPermission(String permission, Permissible permissible);

    /**
     * Subscribes a {@link Permissible} to receive notifications when the given permission changes.
     */
    default void subscribeToPermission(Permission permission, Permissible permissible) {
        subscribeToPermission(permission.getName(), permissible);
    }

    /**
     * Unsubscribes a {@link Permissible} from the named permission.
     */
    void unsubscribeFromPermission(String permission, Permissible permissible);

    /**
     * Unsubscribes a {@link Permissible} from the given permission.
     * Symmetric counterpart to {@link #subscribeToPermission(Permission, Permissible)}.
     */
    default void unsubscribeFromPermission(Permission permission, Permissible permissible) {
        unsubscribeFromPermission(permission.getName(), permissible);
    }

    /**
     * Returns all {@link Permissible}s currently subscribed to the named permission.
     */
    Set<Permissible> getPermissionSubscriptions(String permission);

    /**
     * Subscribes a {@link Permissible} to the default permission set for the given operator status.
     */
    void subscribeToDefaultPerms(boolean op, Permissible permissible);

    /**
     * Unsubscribes a {@link Permissible} from the default permission set for the given operator status.
     */
    void unsubscribeFromDefaultPerms(boolean op, Permissible permissible);

    /**
     * Returns all {@link Permissible}s subscribed to the default set for the given operator status.
     */
    Set<Permissible> getDefaultPermSubscriptions(boolean op);

    /**
     * Returns a snapshot copy of all registered permissions.
     */
    Map<String, Permission> getPermissions();

    /**
     * Returns a sequential stream over all registered permissions.
     */
    default Stream<Permission> permissionStream() {
        return getPermissions().values().stream();
    }
}
