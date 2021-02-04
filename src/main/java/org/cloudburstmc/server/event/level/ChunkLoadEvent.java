package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.level.chunk.CloudChunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChunkLoadEvent extends ChunkEvent {

    private final boolean newChunk;

    public ChunkLoadEvent(CloudChunk chunk, boolean newChunk) {
        super(chunk);
        this.newChunk = newChunk;
    }

    public boolean isNewChunk() {
        return newChunk;
    }
}