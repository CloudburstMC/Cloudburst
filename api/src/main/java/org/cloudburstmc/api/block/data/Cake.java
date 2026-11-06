package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A plain cake's consumed slices. Even at the maximum stored bite count,
 * one slice remains until the cake is eaten again.
 */
public interface Cake extends BlockData {

    /**
     * @return the number of slices already eaten
     */
    int getBites();

    /**
     * @return the greatest supported bite count
     */
    int getMaximumBites();

    /**
     * @param bites the replacement count, from zero through {@link #getMaximumBites()}
     * @return a state with the requested bite count
     * @throws IllegalArgumentException if the count is outside the supported range
     */
    BlockState withBites(int bites);
}
