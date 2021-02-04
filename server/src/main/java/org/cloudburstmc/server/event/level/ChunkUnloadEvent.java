package org.cloudburstmc.server.event.level;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.level.chunk.CloudChunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChunkUnloadEvent extends ChunkEvent implements Cancellable {

    public ChunkUnloadEvent(CloudChunk chunk) {
        super(chunk);
    }

}