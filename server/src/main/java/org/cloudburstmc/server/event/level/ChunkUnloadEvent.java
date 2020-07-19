package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.level.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChunkUnloadEvent extends ChunkEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public ChunkUnloadEvent(Chunk chunk) {
        super(chunk);
    }

}