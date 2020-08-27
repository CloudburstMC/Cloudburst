package org.cloudburstmc.server.metadata;

import org.cloudburstmc.server.level.Level;

import javax.inject.Singleton;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Singleton
public class LevelMetadataStore extends MetadataStore {

    @Override
    protected String disambiguate(Metadatable level, String metadataKey) {
        if (!(level instanceof Level)) {
            throw new IllegalArgumentException("Argument must be a Level instance");
        }
        return (((Level) level).getName() + ":" + metadataKey).toLowerCase();
    }
}
