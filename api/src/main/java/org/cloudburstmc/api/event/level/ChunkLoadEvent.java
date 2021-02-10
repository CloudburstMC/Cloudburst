package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.level.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class ChunkLoadEvent extends ChunkEvent {

    private final boolean newChunk;

    public ChunkLoadEvent(Chunk chunk, boolean newChunk) {
        super(chunk);
        this.newChunk = newChunk;
    }

    public boolean isNewChunk() {
        return newChunk;
    }
}