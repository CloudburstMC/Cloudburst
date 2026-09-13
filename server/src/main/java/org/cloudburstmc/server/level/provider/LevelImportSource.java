package org.cloudburstmc.server.level.provider;

import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Read-only source used to import a level from another storage format.
 */
public interface LevelImportSource extends Closeable {

    /**
     * Loads the source level metadata.
     *
     * @param initialData complete state copied before persisted fields are applied
     * @return future containing source metadata, or empty when none exists
     */
    CompletableFuture<Optional<CloudLevelData>> loadLevelData(CloudLevelData initialData);

    /**
     * Visits every persisted chunk and fails if any chunk cannot be read or consumed.
     *
     * @param factory  creates builders bound to the importing level
     * @param consumer receives each decoded chunk
     * @return future completed after every chunk has been consumed
     */
    CompletableFuture<Void> visitChunks(ChunkBuilder.Factory factory, Consumer<CloudChunk> consumer);
}
