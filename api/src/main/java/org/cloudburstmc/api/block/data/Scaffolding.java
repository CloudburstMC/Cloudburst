package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A scaffold's stored distance from a supported column.
 */
public interface Scaffolding extends BlockData {

    /**
     * @return the support distance, where zero is directly supported
     */
    int getDistance();

    /**
     * @return the distance at which scaffolding loses support
     */
    int getMaximumDistance();

    /**
     * @param distance the distance from zero through {@link #getMaximumDistance()}
     * @return a state with the requested distance
     * @throws IllegalArgumentException if the distance is outside the supported range
     */
    BlockState withDistance(int distance);
}
