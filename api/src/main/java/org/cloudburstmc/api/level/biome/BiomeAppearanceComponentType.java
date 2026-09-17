package org.cloudburstmc.api.level.biome;

import lombok.Value;
import org.cloudburstmc.api.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifies a component that defines biome presentation such as colors, sound, or lighting.
 */
@Value
public class BiomeAppearanceComponentType {
    Identifier id;

    private BiomeAppearanceComponentType(Identifier id) {
        this.id = checkNotNull(id, "id");
    }

    /**
     * Creates a biome appearance component type.
     *
     * @param id component identifier
     * @return component type
     */
    public static BiomeAppearanceComponentType of(Identifier id) {
        return new BiomeAppearanceComponentType(id);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
