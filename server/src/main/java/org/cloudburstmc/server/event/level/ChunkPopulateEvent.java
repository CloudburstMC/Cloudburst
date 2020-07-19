package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.level.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChunkPopulateEvent extends ChunkEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public ChunkPopulateEvent(Chunk chunk) {
        super(chunk);
    }

}