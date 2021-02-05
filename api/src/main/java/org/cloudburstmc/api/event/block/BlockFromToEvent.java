package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.Cancellable;

public class BlockFromToEvent extends BlockEvent implements Cancellable {

    private BlockState to;

    public BlockFromToEvent(Block blockState, BlockState to) {
        super(blockState);
        this.to = to;
    }

    public Block getFrom() {
        return getBlock();
    }

    public BlockState getTo() {
        return to;
    }

    public void setTo(BlockState newTo) {
        to = newTo;
    }
}