package org.cloudburstmc.server.level.biome;

import org.cloudburstmc.api.level.biome.BiomeType;
import org.cloudburstmc.api.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CloudBiomeTest {
    private static final int SEA_LEVEL = 63;
    
    @Test
    void preservesBaseTemperatureThroughSnowLevel() {
        CloudBiome biome = biome(0.8d);
        assertEquals(0.8d, biome.temperatureAt(10, SEA_LEVEL + 17, 20, SEA_LEVEL));
    }

    @Test
    void lowersTemperatureAboveSnowLevel() {
        CloudBiome biome = biome(0.8d);
        assertTrue(biome.temperatureAt(10, SEA_LEVEL + 100, 20, SEA_LEVEL) < biome.temperature());
    }

    @Test
    void usesStrictFreezingThreshold() {
        assertTrue(biome(0.149d).coldEnoughToSnow(0, SEA_LEVEL, 0, SEA_LEVEL));
        assertFalse(biome(0.15d).coldEnoughToSnow(0, SEA_LEVEL, 0, SEA_LEVEL));
    }

    private static CloudBiome biome(double temperature) {
        return new CloudBiome(BiomeType.of(Identifier.parse("test:climate")), Set.of(), temperature, 0.5d, true);
    }
}
