package org.cloudburstmc.api.level.biome;

import org.cloudburstmc.api.util.Identifier;

import java.util.Set;

/**
 * A registered biome and its server-observable properties.
 */
public interface Biome {

    /**
     * Returns this biome's type.
     *
     * @return biome type
     */
    BiomeType getType();

    /**
     * Tests whether this biome has a tag.
     *
     * @param tag tag identifier
     * @return {@code true} when the biome has the tag
     */
    boolean hasTag(Identifier tag);

    /**
     * Returns this biome's tags.
     *
     * @return immutable biome tags
     */
    Set<Identifier> getTags();
}
