package org.cloudburstmc.api.level.biome;

import org.cloudburstmc.api.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A registered kind of biome.
 */
public class BiomeType {
    private final Identifier id;

    private BiomeType(Identifier id) {
        this.id = checkNotNull(id, "id");
    }

    /**
     * Creates a biome type.
     *
     * @param id biome identifier
     * @return biome type
     */
    public static BiomeType of(Identifier id) {
        return new BiomeType(id);
    }

    /**
     * Returns the biome identifier.
     *
     * @return biome identifier
     */
    public Identifier getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "BiomeType{id=" + this.id + '}';
    }
}
