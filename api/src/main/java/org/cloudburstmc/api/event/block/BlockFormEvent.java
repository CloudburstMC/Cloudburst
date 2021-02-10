package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class BlockFormEvent extends BlockGrowEvent implements Cancellable {

    public BlockFormEvent(Block block, BlockState newState) {
        super(block, newState);
    }

}
