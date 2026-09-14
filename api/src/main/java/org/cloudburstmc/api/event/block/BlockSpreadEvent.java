package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;

/**
 * Called before a block spreads to another position.
 */
public class BlockSpreadEvent extends BlockFormEvent {

    private final Block source;

    /**
     * Creates a block spread event.
     *
     * @param block    the block that will change
     * @param source   the block from which the change originated
     * @param newState the proposed state
     */
    public BlockSpreadEvent(Block block, Block source, BlockState newState) {
        super(block, newState);
        this.source = source;
    }

    /**
     * Gets the block from which the change originated.
     *
     * @return the source block
     */
    public Block getSource() {
        return this.source;
    }
}
