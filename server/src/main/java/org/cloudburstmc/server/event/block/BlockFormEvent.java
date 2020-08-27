package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockFormEvent extends BlockGrowEvent implements Cancellable {

    public BlockFormEvent(Block block, BlockState newState) {
        super(block, newState);
    }

}
