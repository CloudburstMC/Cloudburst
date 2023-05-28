package org.cloudburstmc.api.event.redstone;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.block.BlockEvent;

public abstract class RedstoneEvent extends BlockEvent {
    public RedstoneEvent(Block block) {
        super(block);
    }
}
