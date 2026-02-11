package org.cloudburstmc.server.level.generator.standard.store;

import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.reference.cache.Cached;

/**
 * Stores for the various different cacheable resources required for parsing the config for the Cloudburst standard generator.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StandardGeneratorStores {
    private final Cached<GenerationBiomeStore> GENERATION_BIOME_STORE_CACHE = Cached.global(GenerationBiomeStore::new);

    public GenerationBiomeStore generationBiome() {
        return GENERATION_BIOME_STORE_CACHE.get();
    }
}
