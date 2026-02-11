package org.cloudburstmc.server.level.generator.standard.registry;

import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.reference.cache.Cached;

/**
 * Registries for looking up the various different resources required for parsing the config for the Cloudburst standard generator.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StandardGeneratorRegistries {
    private final Cached<BiomeFilterRegistry> BIOME_FILTER_REGISTRY_CACHE = Cached.global(BiomeFilterRegistry::new);
    private final Cached<BiomeMapRegistry> BIOME_MAP_REGISTRY_CACHE = Cached.global(BiomeMapRegistry::new);
    private final Cached<DecoratorRegistry> DECORATOR_REGISTRY_CACHE = Cached.global(DecoratorRegistry::new);
    private final Cached<DensitySourceRegistry> DENSITY_SOURCE_REGISTRY_CACHE = Cached.global(DensitySourceRegistry::new);
    private final Cached<FinisherRegistry> FINISHER_REGISTRY_CACHE = Cached.global(FinisherRegistry::new);
    private final Cached<NoiseGeneratorRegistry> NOISE_GENERATOR_REGISTRY_CACHE = Cached.global(NoiseGeneratorRegistry::new);
    private final Cached<PopulatorRegistry> POPULATOR_REGISTRY_CACHE = Cached.global(PopulatorRegistry::new);

    public BiomeFilterRegistry biomeFilter() {
        return BIOME_FILTER_REGISTRY_CACHE.get();
    }

    public BiomeMapRegistry biomeMap() {
        return BIOME_MAP_REGISTRY_CACHE.get();
    }

    public DecoratorRegistry decorator() {
        return DECORATOR_REGISTRY_CACHE.get();
    }

    public DensitySourceRegistry densitySource() {
        return DENSITY_SOURCE_REGISTRY_CACHE.get();
    }

    public FinisherRegistry finisher() {
        return FINISHER_REGISTRY_CACHE.get();
    }

    public NoiseGeneratorRegistry noiseGenerator() {
        return NOISE_GENERATOR_REGISTRY_CACHE.get();
    }

    public PopulatorRegistry populator() {
        return POPULATOR_REGISTRY_CACHE.get();
    }
}
