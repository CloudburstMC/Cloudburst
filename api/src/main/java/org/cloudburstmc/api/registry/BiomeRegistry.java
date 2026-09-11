package org.cloudburstmc.api.registry;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.biome.Biome;
import org.cloudburstmc.api.util.Identifier;

import java.util.Optional;

/**
 * Registry for biome definitions.
 *
 * @param <T> biome implementation type
 */
public interface BiomeRegistry<T extends Biome> extends KeyedRegistry<T> {

    /**
     * Registers a biome definition.
     *
     * @param biome biome to register
     * @throws RegistryException if registration fails
     */
    void register(T biome) throws RegistryException;

    /**
     * Returns a biome by identifier.
     *
     * @param id biome identifier
     * @return matching biome, or {@code null} if none is registered
     */
    @Nullable
    T getBiome(Identifier id);

    @Override
    default Optional<T> get(Identifier id) {
        return Optional.ofNullable(this.getBiome(id));
    }

    @Override
    default Identifier getId(T value) {
        return value.getId();
    }
}
