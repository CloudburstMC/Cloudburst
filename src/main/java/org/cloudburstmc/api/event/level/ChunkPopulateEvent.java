package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.level.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChunkPopulateEvent extends ChunkEvent {

    public ChunkPopulateEvent(Chunk chunk) {
        super(chunk);
    }

}