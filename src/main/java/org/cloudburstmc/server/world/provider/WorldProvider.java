package org.cloudburstmc.server.world.provider;

import org.cloudburstmc.server.world.WorldData;
import org.cloudburstmc.server.world.chunk.Chunk;
import org.cloudburstmc.server.world.chunk.ChunkBuilder;
import org.cloudburstmc.server.utils.LoadState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Interface that allows the world to load and save chunks from any storage implementation.
 */
@ParametersAreNonnullByDefault
public interface WorldProvider extends PlayerDataProvider, Closeable {

    /**
     * World ID
     *
     * @return id
     */
    String getWorldId();

    /**
     * Reads chunk from provider asynchronously
     *
     * @param chunkBuilder builder
     * @return future when chunk is loaded. Will return null if the chunk does not exist
     */
    CompletableFuture<Chunk> readChunk(ChunkBuilder chunkBuilder);

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
    CompletableFuture<Void> forEachChunk(ChunkBuilder.Factory factory, BiConsumer<Chunk, Throwable> consumer);

    /**
     * Load world data into given {@link WorldData} object
     *
     * @param worldData worldData to load
     * @return future of loaded world data
     */
    CompletableFuture<LoadState> loadLevelData(WorldData worldData);

    /**
     * Save world data from given {@link WorldData} object
     *
     * @param worldData worldData to save
     */
    CompletableFuture<Void> saveLevelData(WorldData worldData);
}
