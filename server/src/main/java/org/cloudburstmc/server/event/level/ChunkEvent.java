package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.level.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class ChunkEvent extends LevelEvent {

    private final Chunk chunk;

    public ChunkEvent(Chunk chunk) {
        super(chunk.getLevel());
        this.chunk = chunk;
    }

    public Chunk getChunk() {
        return chunk;
    }
}
