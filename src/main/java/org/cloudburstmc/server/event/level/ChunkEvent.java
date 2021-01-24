package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.world.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class ChunkEvent extends WorldEvent {

    private final Chunk chunk;

    public ChunkEvent(Chunk chunk) {
        super(chunk.getWorld());
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }
}
