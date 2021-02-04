package org.cloudburstmc.server.event.block;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;

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