package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;

/**
 * Created by CreeperFace on 12.5.2017.
 */
public final class BlockRedstoneEvent extends BlockEvent {

    private final int oldPower;
    private final int newPower;

    public BlockRedstoneEvent(Block block, int oldPower, int newPower) {
        super(block);
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
