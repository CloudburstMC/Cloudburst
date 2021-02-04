package org.cloudburstmc.server.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Event;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class BlockEvent extends Event {

    protected final Block block;

    public BlockEvent(Block block) {
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
