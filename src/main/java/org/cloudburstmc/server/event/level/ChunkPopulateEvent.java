package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.world.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChunkPopulateEvent extends ChunkEvent {

    public ChunkPopulateEvent(Chunk chunk) {
        super(chunk);
    }

}