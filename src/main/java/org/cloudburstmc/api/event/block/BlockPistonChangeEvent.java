package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;

/**
 * Created by CreeperFace on 2.8.2017.
 */
public final class BlockPistonChangeEvent extends BlockEvent {

    private int oldPower;
    private int newPower;

    public BlockPistonChangeEvent(Block block, int oldPower, int newPower) {
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
