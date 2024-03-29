package org.cloudburstmc.server.level.generator.standard.registry;

import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.ref.Ref;

/**
 * Registries for looking up the various different resources required for parsing the config for the Cloudburst standard generator.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StandardGeneratorRegistries {
    private final Ref<BiomeFilterRegistry> BIOME_FILTER_REGISTRY_CACHE = Ref.lazy(BiomeFilterRegistry::new);
    private final Ref<BiomeMapRegistry> BIOME_MAP_REGISTRY_CACHE = Ref.lazy(BiomeMapRegistry::new);
    private final Ref<DecoratorRegistry> DECORATOR_REGISTRY_CACHE = Ref.lazy(DecoratorRegistry::new);
    private final Ref<DensitySourceRegistry> DENSITY_SOURCE_REGISTRY_CACHE = Ref.lazy(DensitySourceRegistry::new);
    private final Ref<FinisherRegistry> FINISHER_REGISTRY_CACHE = Ref.lazy(FinisherRegistry::new);
    private final Ref<NoiseGeneratorRegistry> NOISE_GENERATOR_REGISTRY_CACHE = Ref.lazy(NoiseGeneratorRegistry::new);
    private final Ref<PopulatorRegistry> POPULATOR_REGISTRY_CACHE = Ref.lazy(PopulatorRegistry::new);

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
