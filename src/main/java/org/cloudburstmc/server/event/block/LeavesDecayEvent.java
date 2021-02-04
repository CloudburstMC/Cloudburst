package org.cloudburstmc.server.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class LeavesDecayEvent extends BlockEvent implements Cancellable {

    public LeavesDecayEvent(Block blockState) {
        super(blockState);
    }

}
