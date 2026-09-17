package org.cloudburstmc.api.level.biome;

import lombok.Value;
import org.cloudburstmc.api.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifies a component that defines biome behavior or world generation.
 */
@Value
public class BiomeBehaviorComponentType {
    Identifier id;

    private BiomeBehaviorComponentType(Identifier id) {
        this.id = checkNotNull(id, "id");
    }

    /**
     * Creates a biome behavior component type.
     *
     * @param id component identifier
     * @return component type
     */
    public static BiomeBehaviorComponentType of(Identifier id) {
        return new BiomeBehaviorComponentType(id);
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
