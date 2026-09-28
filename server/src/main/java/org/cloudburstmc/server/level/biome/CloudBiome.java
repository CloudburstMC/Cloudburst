package org.cloudburstmc.server.level.biome;

import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.PerlinNoiseEngine;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.level.biome.Biome;
import org.cloudburstmc.api.level.biome.BiomeType;
import org.cloudburstmc.api.util.Identifier;

import java.util.Objects;
import java.util.Set;

public record CloudBiome(BiomeType type, Set<Identifier> tags, double temperature, double downfall, boolean hasPrecipitation) implements Biome {
    private static final double FREEZING_TEMPERATURE = 0.15d;
    private static final NoiseSource TEMPERATURE_NOISE = new PerlinNoiseEngine(new FastPRandom(123456789L));

    public CloudBiome {
        Objects.requireNonNull(type, "type");
        tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
    }

    @Override
    public BiomeType getType() {
        return this.type;
    }

    @Override
    public boolean hasTag(Identifier tag) {
        return this.tags.contains(tag);
    }

    @Override
    public Set<Identifier> getTags() {
        return this.tags;
    }

    public double temperatureAt(int x, int y, int z, int seaLevel) {
        int snowLevel = seaLevel + 17;
        if (y > snowLevel) {
            double noise = TEMPERATURE_NOISE.get(x / 8.0d, z / 8.0d) * 8.0d;
            return this.temperature - (noise + y - snowLevel) * 0.00125d;
        }

        return this.temperature;
    }

    public boolean coldEnoughToSnow(int x, int y, int z, int seaLevel) {
        return this.temperatureAt(x, y, z, seaLevel) < FREEZING_TEMPERATURE;
    }
}
