package org.cloudburstmc.server.level.generator.standard.store;

import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.ref.Ref;

/**
 * Stores for the various different cacheable resources required for parsing the config for the Cloudburst standard generator.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StandardGeneratorStores {
    private final Ref<GenerationBiomeStore> GENERATION_BIOME_STORE_CACHE = Ref.lazy(GenerationBiomeStore::new);

    public GenerationBiomeStore generationBiome() {
        return GENERATION_BIOME_STORE_CACHE.get();
    }
}
