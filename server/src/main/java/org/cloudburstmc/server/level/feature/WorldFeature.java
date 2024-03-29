package org.cloudburstmc.server.level.feature;

import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.level.ChunkManager;

import java.util.random.RandomGenerator;

/**
 * @author DaPorkchop_
 */
@FunctionalInterface
public interface WorldFeature {
    /**
     * Tries to place this feature into the given level at the given position.
     *
     * @param level  the level to place the feature into
     * @param random an instance of {@link PRandom} for generating random numbers
     * @param x      the X coordinate to generate the feature at
     * @param y      the Y coordinate to generate the feature at
     * @param z      the Z coordinate to generate the feature at
     * @return whether or not the feature could be placed
     */
    boolean place(ChunkManager level, RandomGenerator random, int x, int y, int z);
}
