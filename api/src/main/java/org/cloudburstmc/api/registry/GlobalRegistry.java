package org.cloudburstmc.api.registry;

/**
 * Provides access to server registries by registry type.
 */
public interface GlobalRegistry {

    /**
     * Gets a registry by its API type.
     *
     * @param registryClass the registry interface or implementation class
     * @return the matching registry
     */
    Registry getRegistry(Class<? extends Registry> registryClass);
}
