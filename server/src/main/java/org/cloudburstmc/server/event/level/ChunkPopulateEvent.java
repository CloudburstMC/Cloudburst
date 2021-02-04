package org.cloudburstmc.server.event.level;

import org.cloudburstmc.server.level.chunk.CloudChunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChunkPopulateEvent extends ChunkEvent {

    public ChunkPopulateEvent(CloudChunk chunk) {
        super(chunk);
    }

}