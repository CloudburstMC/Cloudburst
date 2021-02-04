package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.level.chunk.CloudChunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class ChunkEvent extends LevelEvent {

    private final CloudChunk chunk;

    public ChunkEvent(CloudChunk chunk) {
        super(chunk.getLevel());
        this.chunk = chunk;
    }

    public CloudChunk getChunk() {
        return chunk;
    }
}
