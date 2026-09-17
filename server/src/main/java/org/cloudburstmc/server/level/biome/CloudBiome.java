package org.cloudburstmc.server.level.biome;

import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.PerlinNoiseEngine;
import net.daporkchop.lib.random.impl.FastPRandom;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.level.biome.Biome;
import org.cloudburstmc.api.level.biome.BiomeType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.generator.BlockStateRegion;

import java.util.Objects;
import java.util.Set;

public record CloudBiome(BiomeType type, Set<Identifier> tags, double temperature, double downfall) implements Biome {
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

    public double temperatureAt(int x, int y, int z) {
        double temperature = this.temperature;
        if (y > 64) {
            double noise = TEMPERATURE_NOISE.get(x * 0.125d, z * 0.125d) * 4.0d;
            temperature -= (noise + y - 64.0d) * 0.001666666666d;
        }
        return temperature;
    }

    public boolean canSnowAt(BlockStateRegion level, int x, int y, int z) {
        //TODO: light level must be less than 10
        return y >= 0 && this.temperatureAt(x, y, z) < 0.15d && (y >= 256 || level.getBlockState(x, y, z) == BlockStates.AIR);
    }
}
