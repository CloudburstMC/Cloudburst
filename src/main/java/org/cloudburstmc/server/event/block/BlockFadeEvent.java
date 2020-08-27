package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;

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
