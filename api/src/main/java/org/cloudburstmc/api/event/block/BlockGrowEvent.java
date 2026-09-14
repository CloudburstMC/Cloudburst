package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Called before a block grows naturally.
 */
public class BlockGrowEvent extends BlockEvent implements Cancellable {

    private BlockState newState;

    /**
     * Creates a block growth event.
     *
     * @param block    the block that will change
     * @param newState the proposed state
     */
    public BlockGrowEvent(Block block, BlockState newState) {
        super(block);
        this.newState = Objects.requireNonNull(newState, "newState");
    }

    /**
     * Gets the proposed state.
     *
     * @return the proposed state
     */
    public BlockState getNewState() {
        return this.newState;
    }

    /**
     * Sets the state to apply when the event is not canceled.
     *
     * @param newState the proposed state
     */
    public void setNewState(BlockState newState) {
        this.newState = Objects.requireNonNull(newState, "newState");
    }

}
