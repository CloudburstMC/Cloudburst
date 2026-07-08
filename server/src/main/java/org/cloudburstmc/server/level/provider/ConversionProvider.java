package org.cloudburstmc.server.level.provider;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.server.level.LevelData;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.utils.LoadState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class ConversionProvider implements LevelProvider {
    private final LevelProvider newChunkProvider;
    private final LevelProvider oldChunkProvider;

    public ConversionProvider(LevelProvider newChunkProvider, LevelProvider oldChunkProvider) {
        this.newChunkProvider = newChunkProvider;
        this.oldChunkProvider = oldChunkProvider;
    }

    @Override
    public String getLevelId() {
        return newChunkProvider.getLevelId();
    }

    @Override
    @Nullable
    public CloudChunk readChunk(ChunkBuilder chunkBuilder) {
        CloudChunk chunk = this.newChunkProvider.readChunk(chunkBuilder);
        if (chunk != null) {
            return chunk;
        }

        CloudChunk oldChunk = this.oldChunkProvider.readChunk(chunkBuilder);
        if (oldChunk != null) {
            oldChunk.setDirty();
        }

        return oldChunk;
    }

    @Override
    public CompletableFuture<Void> saveChunk(Chunk chunk) {
        return this.newChunkProvider.saveChunk(chunk);
    }

    @Override
    public CompletableFuture<Void> forEachChunk(ChunkBuilder.Factory factory, BiConsumer<CloudChunk, Throwable> consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<LoadState> loadLevelData(LevelData levelData) {
        return this.newChunkProvider.loadLevelData(levelData).thenCompose(loadState -> {
            if (loadState == LoadState.NOT_FOUND) {
                return this.oldChunkProvider.loadLevelData(levelData);
            }
            return CompletableFuture.completedFuture(loadState);
        });
    }

    @Override
    public CompletableFuture<Void> saveLevelData(LevelData levelData) {
        return this.newChunkProvider.saveLevelData(levelData);
    }

    @Override
    public void close() throws IOException {
        this.newChunkProvider.close();
        this.oldChunkProvider.close();
    }
}
