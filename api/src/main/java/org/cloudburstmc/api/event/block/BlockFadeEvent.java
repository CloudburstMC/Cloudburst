package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Fired before a block fades, melts, dies, or hatches.
 * Cancellation prevents the proposed state change.
 */
public class BlockFadeEvent extends BlockEvent implements Cancellable {

    private BlockState newState;

    /**
     * Creates a block fade event.
     *
     * @param block    the block that will change
     * @param newState the proposed replacement state
     */
    public BlockFadeEvent(Block block, BlockState newState) {
        super(block);
        this.newState = Objects.requireNonNull(newState, "newState");
    }

    /**
     * Gets the proposed replacement state.
     *
     * @return the proposed replacement state
     */
    public BlockState getNewState() {
        return this.newState;
    }

    /**
     * Sets the state to apply unless the event is canceled.
     *
     * @param newState the replacement state
     */
    public void setNewState(BlockState newState) {
        this.newState = Objects.requireNonNull(newState, "newState");
    }
}
