package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.level.biome.*;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BiomeCatalogTest {
    @Test
    void resolvesEveryBuiltInBiomeByIdentifier() {
        BiomeTypes.values().forEach(type -> assertSame(type, BiomeTypes.get(type.getId()).orElseThrow()));
    }

    @Test
    void registersEveryBuiltInBiome() {
        CloudBiomeRegistry registry = CloudBiomeRegistry.get();
        assertEquals(BiomeTypes.values().size(), registry.values().size());
        BiomeTypes.values().forEach(type -> {
            CloudBiome biome = requireNonNull(registry.getBiome(type));
            assertSame(type, biome.getType());
        });
    }

    @Test
    void resolvesEveryBuiltInComponentByIdentifier() {
        BiomeBehaviorComponentTypes.values().forEach(type -> assertSame(type, BiomeBehaviorComponentTypes.get(type.getId()).orElseThrow()));
        BiomeAppearanceComponentTypes.values().forEach(type -> assertSame(type, BiomeAppearanceComponentTypes.get(type.getId()).orElseThrow()));
    }

    @Test
    void permitsCustomComponentNamespaces() {
        Identifier behaviorId = Identifier.parse("test:surface_rules");
        Identifier appearanceId = Identifier.parse("test:visuals");

        assertEquals(behaviorId, BiomeBehaviorComponentType.of(behaviorId).getId());
        assertEquals(appearanceId, BiomeAppearanceComponentType.of(appearanceId).getId());
    }
}
