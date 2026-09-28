package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.level.chunk.Chunk;

/**
 * Called on the server thread after a chunk finishes population and becomes
 * available in its level. Follows {@link ChunkLoadEvent} for that load.
 * This includes the first live load of saved unfinished generation.
 * Reading an already finished chunk from storage does not fire this event.
 */
public final class ChunkPopulateEvent extends ChunkEvent {

    /**
     * @param chunk populated chunk
     */
    public ChunkPopulateEvent(Chunk chunk) {
        super(chunk);
    }
}
