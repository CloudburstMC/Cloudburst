package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

public class BlockFadeEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final BlockState newState;

    public BlockFadeEvent(BlockState blockState, BlockState newState) {
        super(blockState);
        this.newState = newState;
    }

    public BlockState getNewState() {
        return newState;
    }
}
