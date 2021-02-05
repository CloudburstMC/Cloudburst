package org.cloudburstmc.api.event.redstone;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.block.BlockUpdateEvent;

public class RedstoneUpdateEvent extends BlockUpdateEvent {

    public RedstoneUpdateEvent(Block source) {
        super(source);
    }

}

