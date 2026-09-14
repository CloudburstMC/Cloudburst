package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;

/**
 * Called before a block forms because of level conditions.
 */
public class BlockFormEvent extends BlockGrowEvent {

    /**
     * Creates a block formation event.
     *
     * @param block    the block that will change
     * @param newState the proposed state
     */
    public BlockFormEvent(Block block, BlockState newState) {
        super(block, newState);
    }
}
