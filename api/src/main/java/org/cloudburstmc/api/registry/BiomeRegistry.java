package org.cloudburstmc.api.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.biome.Biome;
import org.cloudburstmc.api.level.biome.BiomeType;
import org.cloudburstmc.api.util.Identifier;

import java.util.Optional;

/**
 * Registry for biome definitions.
 */
public interface BiomeRegistry extends KeyedRegistry<Biome> {

    /**
     * Returns a biome by type.
     *
     * @param type biome type
     * @return matching biome, or {@code null} if none is registered
     */
    @Nullable
    Biome getBiome(BiomeType type);

    @Override
    default Optional<Biome> get(Identifier id) {
        return Optional.ofNullable(this.getBiome(BiomeType.of(id)));
    }

    @Override
    default Identifier getId(Biome value) {
        return value.getType().getId();
    }
}
