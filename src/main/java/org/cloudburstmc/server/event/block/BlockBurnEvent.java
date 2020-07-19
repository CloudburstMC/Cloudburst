package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBurnEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public BlockBurnEvent(BlockState blockState) {
        super(blockState);
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

}
