package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A block with an open state.
 */
public interface Openable extends BlockData {

    /**
     * @return whether the block is open
     */
    boolean isOpen();

    /**
     * @param open whether the resulting state is open
     * @return a state with the requested open value
     */
    BlockState withOpen(boolean open);
}
