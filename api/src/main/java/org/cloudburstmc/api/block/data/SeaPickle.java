package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A cluster of sea pickles.
 */
public interface SeaPickle extends BlockData {

    /**
     * @return the number of sea pickles
     */
    int getPickles();

    /**
     * @return the smallest supported pickle count
     */
    int getMinimumPickles();

    /**
     * @return the greatest supported pickle count
     */
    int getMaximumPickles();

    /**
     * @param pickles the replacement count within the supported bounds
     * @return a state with the requested number of sea pickles
     * @throws IllegalArgumentException if the count is outside the supported range
     */
    BlockState withPickles(int pickles);
}
