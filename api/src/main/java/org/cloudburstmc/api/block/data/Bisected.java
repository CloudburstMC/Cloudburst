package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.data.BlockHalf;

/**
 * A block occupying a vertical half or forming part of a two-block structure.
 */
public interface Bisected extends BlockData {

    /**
     * @return the represented half
     */
    BlockHalf getHalf();

    /**
     * Changes only this state, not the other block of a two-block structure.
     *
     * @param half the replacement half
     * @return a state representing the requested half
     */
    BlockState withHalf(BlockHalf half);
}
