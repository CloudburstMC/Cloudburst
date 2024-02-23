package org.cloudburstmc.api.level.biome;

import org.cloudburstmc.api.util.Identifier;

import java.util.Set;

public interface Biome {

    Identifier getId();

    boolean hasTag(Identifier tag);

    Set<Identifier> getTags();
}