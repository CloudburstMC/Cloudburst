package org.cloudburstmc.server.level.provider;

import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.server.level.LevelData;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.utils.LoadState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Interface that allows the level to load and save chunks from any storage implementation.
 */
@ParametersAreNonnullByDefault
public interface LevelProvider extends PlayerDataProvider, Closeable {

    /**
     * Level ID
     *
     * @return id
     */
    String getLevelId();

    /**
     * Reads chunk from provider asynchronously
     *
     * @param chunkBuilder builder
     * @return future when chunk is loaded. Will return null if the chunk does not exist
     */
    CompletableFuture<CloudChunk> readChunk(ChunkBuilder chunkBuilder);

    /**
     * Saves chunk to provider asynchronously
     *
     * @param chunk chunk
     * @return void future when chunk is saved.
     */
    CompletableFuture<Void> saveChunk(Chunk chunk);

    /**
     * Iterate over all chunks that the provider has.
     *
     * @param consumer
     * @throws UnsupportedOperationException if the provider does not support chunk iteration.
     */
    CompletableFuture<Void> forEachChunk(ChunkBuilder.Factory factory, BiConsumer<CloudChunk, Throwable> consumer);

    /**
     * Load level data into given {@link LevelData} object
     *
     * @param levelData levelData to load
     * @return future of loaded level data
     */
    CompletableFuture<LoadState> loadLevelData(LevelData levelData);

    /**
     * Save level data from given {@link LevelData} object
     *
     * @param levelData levelData to save
     */
    CompletableFuture<Void> saveLevelData(LevelData levelData);
}
