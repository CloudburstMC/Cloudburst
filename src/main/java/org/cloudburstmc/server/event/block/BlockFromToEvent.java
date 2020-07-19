package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

public class BlockFromToEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private BlockState to;

    public BlockFromToEvent(BlockState blockState, BlockState to) {
        super(blockState);
        this.to = to;
    }

    public BlockState getFrom() {
        return getBlock();
    }

    public BlockState getTo() {
        return to;
    }

    public void setTo(BlockState newTo) {
        to = newTo;
    }
}