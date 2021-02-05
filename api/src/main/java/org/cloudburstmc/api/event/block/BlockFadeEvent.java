package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.Cancellable;

public class BlockFadeEvent extends BlockEvent implements Cancellable {


    private final BlockState newState;

    public BlockFadeEvent(Block blockState, BlockState newState) {
        super(blockState);
        this.newState = newState;
    }

    public BlockState getNewState() {
        return newState;
    }
}
