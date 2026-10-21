package org.cloudburstmc.server.level.provider;

import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilderFactory;

import java.io.Closeable;
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
     * @return future containing source metadata, failing if it is missing or invalid
     */
    CompletableFuture<CloudLevelData> loadLevelData(CloudLevelData initialData);

    /**
     * Visits every persisted chunk and fails if any chunk cannot be read or consumed.
     * The source closes each imported chunk after the consumer returns.
     *
     * @param factory  creates builders bound to the importing level
     * @param consumer finishes reading or saving each decoded chunk before returning
     * @return future completed after every chunk has been consumed
     */
    CompletableFuture<Void> visitChunks(CloudChunkBuilderFactory factory, Consumer<ImportedChunk> consumer);
}
