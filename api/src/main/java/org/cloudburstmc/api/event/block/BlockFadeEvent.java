package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.block.BlockState;

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
