package org.cloudburstmc.server.event.block;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.block.Block;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBurnEvent extends BlockEvent implements Cancellable {

    public BlockBurnEvent(Block block) {
        super(block);
    }


}
