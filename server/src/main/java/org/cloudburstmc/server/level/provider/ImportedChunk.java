package org.cloudburstmc.server.level.provider;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;

/**
 * Owns converted terrain and immutable entity record lists without spawning entities.
 * Entity chunk assignments are finalized after all terrain is saved.
 * Closing this record releases its chunk.
 *
 * @param chunk         converted terrain owned by this record
 * @param entities      entity records whose final chunks are determined from their positions
 * @param blockEntities block entity records belonging to this chunk
 * @param pendingTicks  converted pending ticks for this chunk
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
