package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class BlockBurnEvent extends BlockEvent implements Cancellable {

    public BlockBurnEvent(Block block) {
        super(block);
    }


}
