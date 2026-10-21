package org.cloudburstmc.server.level.provider;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.List;
import java.util.Objects;
import java.io.Closeable;

/**
 * Converted terrain and entity records ready for persistence without spawning entities.
 */
public record ImportedChunk(CloudChunk chunk, List<NbtMap> entities, List<NbtMap> blockEntities, NbtMap pendingTicks) implements Closeable {

    public ImportedChunk {
        Objects.requireNonNull(chunk, "chunk");
        entities = List.copyOf(entities);
        blockEntities = List.copyOf(blockEntities);
        Objects.requireNonNull(pendingTicks, "pendingTicks");
    }

    @Override
    public void close() {
        this.chunk.close();
    }
}
