package org.cloudburstmc.server.event.redstone;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.event.block.BlockUpdateEvent;

/**
 * author: Angelic47
 * Nukkit Project
 */
public class RedstoneUpdateEvent extends BlockUpdateEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public RedstoneUpdateEvent(BlockState source) {
        super(source);
    }

}

