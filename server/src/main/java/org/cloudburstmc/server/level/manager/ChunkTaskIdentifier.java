package org.cloudburstmc.server.level.manager;

import org.cloudburstmc.server.level.chunk.CloudChunk;

public record ChunkTaskIdentifier(ChunkTaskType type, long chunkKey) {

    public int chunkX() {
        return CloudChunk.fromKeyX(chunkKey);
    }

    public int chunkZ() {
        return CloudChunk.fromKeyZ(chunkKey);
    }
}
