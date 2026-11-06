package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A typed view of an immutable block state.
 * Changes return a block state and do not update a block in a level.
 */
public interface BlockData {

    /**
     * @return the state represented by this view
     */
    BlockState getState();
}
