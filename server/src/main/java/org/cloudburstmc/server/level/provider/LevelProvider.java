package org.cloudburstmc.server.level.provider;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;

import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Provides persistent storage for one open level.
 *
 * <p>The provider owns its storage resources until {@link #close()} is called.
 */
public interface LevelProvider extends PlayerDataProvider, Closeable {

    /**
     * Loads a chunk using the supplied builder as its construction context.
     *
     * @param chunkBuilder identifies the chunk and receives its persisted state
     * @return the loaded chunk, or {@code null} when no chunk is stored at those coordinates
     */
    @Nullable
    CloudChunk readChunk(CloudChunkBuilder chunkBuilder);

    /**
     * Persists the complete state of a chunk asynchronously.
     *
     * @param chunk chunk to persist
     * @return future completed after the chunk is persisted
     */
    CompletableFuture<Void> saveChunk(Chunk chunk);

    /**
     * Atomically persists converted terrain and entity records without loading entities.
     * The caller retains ownership of the imported chunk and must keep it open until completion.
     *
     * @param imported converted chunk whose destination must not already contain a chunk
     * @return future completed after all chunk records are persisted
     */
    CompletableFuture<Void> saveImportedChunk(ImportedChunk imported);

    /**
     * Finalizes imported records after all terrain has been saved.
     * Await completion before opening the destination level for normal use.
     *
     * @return future completed after entities are stored in their position's chunks
     */
    CompletableFuture<Void> completeImport();

    /**
     * Persists a chunk's pending ticks without performing a complete chunk save.
     * Providers that cannot store pending ticks independently may retain the no-op implementation.
     *
     * @param chunk chunk whose pending ticks should be persisted
     * @return future completed after the pending ticks are persisted
     */
    default CompletableFuture<Void> savePendingTicks(CloudChunk chunk) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Loads the level's persisted state asynchronously.
     *
     * @param initialData complete baseline for fields absent from storage
     * @return future containing the loaded state, or an empty value when no state is stored
     */
    CompletableFuture<Optional<CloudLevelData>> loadLevelData(CloudLevelData initialData);

    /**
     * Persists the level's current state asynchronously.
     *
     * @param levelData level state to persist
     * @return future completed after the level state is persisted
     */
    CompletableFuture<Void> saveLevelData(CloudLevelData levelData);
}
