package org.cloudburstmc.api.registry;

/**
 * Base contract for registries that have a registration phase.
 */
public interface Registry {

    /**
     * Closes registration and prepares the registry for normal use.
     *
     * @throws RegistryException if the registry cannot be closed
     */
    void close() throws RegistryException;
}
