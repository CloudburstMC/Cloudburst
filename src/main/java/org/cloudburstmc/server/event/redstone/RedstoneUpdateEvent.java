package org.cloudburstmc.server.event.redstone;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.event.block.BlockUpdateEvent;

public class RedstoneUpdateEvent extends BlockUpdateEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public RedstoneUpdateEvent(Block source) {
        super(source);
    }

}

