package org.cloudburstmc.server.level.chunk;

/**
 * The completed generation stage of a chunk.
 */
public enum ChunkGenerationStatus {
    NEW,
    GENERATED,
    POPULATED,
    FINISHED;

    public boolean isAtLeast(ChunkGenerationStatus status) {
        return this.compareTo(status) >= 0;
    }
}
