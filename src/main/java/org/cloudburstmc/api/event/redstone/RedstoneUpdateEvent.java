package org.cloudburstmc.api.event.redstone;

import org.cloudburstmc.api.block.Block;

public final class RedstoneUpdateEvent extends RedstoneEvent {

    public RedstoneUpdateEvent(Block source) {
        super(source);
    }

}

