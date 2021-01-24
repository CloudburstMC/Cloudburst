package org.cloudburstmc.server.world.provider;

import org.cloudburstmc.server.world.WorldData;
import org.cloudburstmc.server.world.chunk.Chunk;
import org.cloudburstmc.server.world.chunk.ChunkBuilder;
import org.cloudburstmc.server.utils.LoadState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class ConversionProvider implements WorldProvider {
    private final WorldProvider newChunkProvider;
    private final WorldProvider oldChunkProvider;

    public ConversionProvider(WorldProvider newChunkProvider, WorldProvider oldChunkProvider) {
        this.newChunkProvider = newChunkProvider;
        this.oldChunkProvider = oldChunkProvider;
    }

    @Override
    public String getWorldId() {
        return newChunkProvider.getWorldId();
    }

    @Override
    public CompletableFuture<Chunk> readChunk(ChunkBuilder chunkBuilder) {
        return this.newChunkProvider.readChunk(chunkBuilder).thenCompose(chunk -> {
            if (chunk == null) {
                // Couldn't find chunk in new provider so lets check the old one
                return this.oldChunkProvider.readChunk(chunkBuilder).thenApply(oldChunk -> {
                    // This chunk must be saved with the new provider.
                    if (oldChunk != null) {
                        oldChunk.setDirty();
                    }
                    return oldChunk;
                });
            }
            return CompletableFuture.completedFuture(chunk);
        });
    }

    @Override
    public CompletableFuture<Void> saveChunk(Chunk chunk) {
        return this.newChunkProvider.saveChunk(chunk);
    }

    @Override
    public CompletableFuture<Void> forEachChunk(ChunkBuilder.Factory factory, BiConsumer<Chunk, Throwable> consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<LoadState> loadLevelData(WorldData worldData) {
        return this.newChunkProvider.loadLevelData(worldData).thenCompose(loadState -> {
            if (loadState == LoadState.NOT_FOUND) {
                return this.oldChunkProvider.loadLevelData(worldData);
            }
            return CompletableFuture.completedFuture(loadState);
        });
    }

    @Override
    public CompletableFuture<Void> saveLevelData(WorldData worldData) {
        return this.newChunkProvider.saveLevelData(worldData);
    }

    @Override
    public void close() throws IOException {
        this.newChunkProvider.close();
        this.oldChunkProvider.close();
    }
}
