package org.cloudburstmc.server.metadata;

import org.cloudburstmc.server.world.World;

import javax.inject.Singleton;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Singleton
public class LevelMetadataStore extends MetadataStore {

    @Override
    protected String disambiguate(Metadatable level, String metadataKey) {
        if (!(level instanceof World)) {
            throw new IllegalArgumentException("Argument must be a World instance");
        }
        return (((World) level).getName() + ":" + metadataKey).toLowerCase();
    }
}
