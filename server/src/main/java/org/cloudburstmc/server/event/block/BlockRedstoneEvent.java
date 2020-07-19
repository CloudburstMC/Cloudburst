package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.HandlerList;

/**
 * Created by CreeperFace on 12.5.2017.
 */
public class BlockRedstoneEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private int oldPower;
    private int newPower;

    public BlockRedstoneEvent(BlockState blockState, int oldPower, int newPower) {
        super(blockState);
        this.oldPower = oldPower;
        this.newPower = newPower;
    }

    public int getOldPower() {
        return oldPower;
    }

    public int getNewPower() {
        return newPower;
    }
}
