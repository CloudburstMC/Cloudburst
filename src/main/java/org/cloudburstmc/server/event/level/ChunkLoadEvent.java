package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.world.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChunkLoadEvent extends ChunkEvent {

    private final boolean newChunk;

    public ChunkLoadEvent(Chunk chunk, boolean newChunk) {
        super(chunk);
        this.newChunk = newChunk;
    }

    public boolean isNewChunk() {
        return newChunk;
    }
}