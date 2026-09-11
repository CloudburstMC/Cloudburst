package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.pack.ResourcePack;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Registry of resource packs available to connecting players.
 */
public interface ResourcePackRegistry extends Registry {

    /**
     * Finds a resource pack by its pack UUID.
     *
     * @param id pack UUID from its manifest
     * @return matching resource pack, if registered
     */
    Optional<ResourcePack> get(UUID id);

    /**
     * Returns all registered resource packs.
     *
     * @return immutable collection of resource packs
     */
    Collection<ResourcePack> values();
}
