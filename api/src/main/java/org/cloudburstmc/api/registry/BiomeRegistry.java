package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.level.biome.Biome;
import org.cloudburstmc.api.util.Identifier;

public interface BiomeRegistry<T extends Biome> extends Registry<T> {

    void register(T biome) throws RegistryException;

    int getRuntimeId(T biome);

    int getRuntimeId(Identifier id);

    T getBiome(Identifier id);

    T getBiome(int runtimeId);

    Identifier getId(int runtimeId);

}