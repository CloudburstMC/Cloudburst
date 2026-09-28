package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.level.chunk.Chunk;

/**
 * Called on the server thread after a chunk becomes available for level ticking
 * and plugin access. The chunk is loaded during this event.
 */
public final class ChunkLoadEvent extends ChunkEvent {

    private final boolean newChunk;

    /**
     * @param chunk    loaded chunk
     * @param newChunk whether the chunk completed generation for this load
     */
    public ChunkLoadEvent(Chunk chunk, boolean newChunk) {
        super(chunk);
        this.newChunk = newChunk;
    }

    /**
     * Whether generation was completed for this load, including unfinished
     * generation resumed from storage.
     *
     * @return {@code true} for the first live load after generation
     */
    public boolean isNewChunk() {
        return newChunk;
    }
}
