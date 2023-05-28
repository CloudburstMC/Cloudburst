package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockGrowEvent extends BlockEvent implements Cancellable {

    private final BlockState newState;

    public BlockGrowEvent(Block block, BlockState newState) {
        super(block);
        this.newState = newState;
    }

    public BlockState getNewState() {
        return newState;
    }

}
