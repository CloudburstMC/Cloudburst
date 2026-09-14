package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Called before a block reacts to a change in a nearby block.
 *
 * <p>This is a high-frequency event. Cancelling it can leave blocks in states
 * that their normal survival rules do not permit.</p>
 */
public class BlockPhysicsEvent extends BlockEvent implements Cancellable {

    private final BlockState changedState;
    private final Block sourceBlock;

    /**
     * Creates a block physics event.
     *
     * @param block        the block receiving the physics update
     * @param changedState the state of that block when the update began
     * @param sourceBlock  the block that caused the update
     */
    public BlockPhysicsEvent(Block block, BlockState changedState, Block sourceBlock) {
        super(block);
        this.changedState = Objects.requireNonNull(changedState, "changedState");
        this.sourceBlock = Objects.requireNonNull(sourceBlock, "sourceBlock");
    }

    /**
     * Gets the state of the affected block when the update began.
     *
     * @return the affected block state
     */
    public BlockState getChangedState() {
        return this.changedState;
    }

    /**
     * Gets the block that caused the update.
     *
     * @return the source block
     */
    public Block getSourceBlock() {
        return this.sourceBlock;
    }
}
