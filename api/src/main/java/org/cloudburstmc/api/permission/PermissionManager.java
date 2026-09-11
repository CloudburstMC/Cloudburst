package org.cloudburstmc.api.permission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Owns the permission definitions used by the server.
 */
public interface PermissionManager {

    /**
     * Finds a registered permission.
     *
     * @param name permission node name
     * @return the definition, if registered
     */
    Optional<Permission> getPermission(String name);

    /**
     * Registers a permission definition.
     *
     * @param permission definition to register
     * @throws IllegalArgumentException if its name is already registered
     */
    default void register(Permission permission) {
        registerAll(List.of(permission));
    }

    /**
     * Registers permission definitions as one validated operation.
     *
     * @param permissions definitions to register
     * @throws IllegalArgumentException if a name is duplicated or the resulting graph contains
     *                                  a missing child or inheritance cycle
     */
    void registerAll(Collection<Permission> permissions);

    /**
     * Replaces an existing permission definition atomically.
     *
     * @param permission replacement definition
     * @throws IllegalArgumentException if its name is not registered or the resulting graph contains
     *                                  a missing child or inheritance cycle
     */
    void replace(Permission permission);

    /**
     * Removes a permission definition.
     *
     * <p>References to the removed node are also removed from registered parent definitions.</p>
     *
     * @param name permission node name
     * @return the removed definition, if registered
     */
    Optional<Permission> unregister(String name);

    /**
     * Returns an immutable snapshot of registered permission definitions in registration order.
     *
     * @return registered definitions
     */
    Collection<Permission> permissions();
}
