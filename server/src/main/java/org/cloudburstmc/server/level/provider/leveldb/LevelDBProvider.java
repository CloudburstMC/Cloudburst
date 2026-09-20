package org.cloudburstmc.server.level.provider.leveldb;

import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;
import net.daporkchop.ldbjni.LevelDB;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.ldbjni.direct.DirectWriteBatch;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.LockedChunk;
import org.cloudburstmc.server.level.provider.LevelProvider;
import org.cloudburstmc.server.level.provider.leveldb.serializer.*;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Log4j2
@ParametersAreNonnullByDefault
public class LevelDBProvider implements LevelProvider {

    private static final int CURRENT_CHUNK_VERSION = 42;
    private static final long CACHE_SIZE = 32L * 1024L * 1024L;
    private static final int MAX_OPEN_FILES = 64;

    /**
     * Maximum number of attempts when a chunk write fails transiently.
     */
    private static final int SAVE_MAX_ATTEMPTS = 5;

    private final Path path;
    private final Executor executor;
    private final DirectDB db;
    private volatile boolean closed;

    public LevelDBProvider(String levelId, Path worldPath, Executor executor) throws IOException {
        this.path = worldPath.resolve(levelId);
        this.executor = executor;
        Path dbPath = this.path.resolve("db");
        Files.createDirectories(dbPath);
        Preconditions.checkArgument(Files.isDirectory(dbPath), "db is not a directory");

        Options options = new Options()
                .createIfMissing(true)
                .compressionType(CompressionType.ZLIB_RAW)
                .blockSize(64 * 1024)
                .cacheSize(CACHE_SIZE)
                .maxOpenFiles(MAX_OPEN_FILES);
        this.db = LevelDB.PROVIDER.open(dbPath.toFile(), options);
    }

    @Override
    @Nullable
    public CloudChunk readChunk(CloudChunkBuilder chunkBuilder) {
        checkForClosed();
        int x = chunkBuilder.getX();
        int z = chunkBuilder.getZ();

        byte[] versionValue = this.db.get(LevelDBKey.VERSION.getKey(x, z));
        if (versionValue == null || versionValue.length != 1) {
            versionValue = this.db.get(LevelDBKey.VERSION_OLD.getKey(x, z));
        }

        if (versionValue == null || versionValue.length != 1) {
            return null;
        }

        byte[] finalizationState = this.db.get(LevelDBKey.STATE_FINALIZATION.getKey(x, z));
        if (finalizationState == null) {
            chunkBuilder.setGenerationStatus(ChunkGenerationStatus.FINISHED);
        } else {
            int stateValue = (finalizationState[0] & 0xFF)
                    | ((finalizationState[1] & 0xFF) << 8)
                    | ((finalizationState[2] & 0xFF) << 16)
                    | ((finalizationState[3] & 0xFF) << 24);
            int statusOrdinal = Math.clamp(stateValue + 1, 0, ChunkGenerationStatus.FINISHED.ordinal());
            chunkBuilder.setGenerationStatus(ChunkGenerationStatus.values()[statusOrdinal]);
        }

        byte chunkVersion = versionValue[0];
        if (chunkVersion < 7) {
            chunkBuilder.markDirty();
        }

        chunkBuilder.setStorageVersion(chunkVersion & 0xFF);
        ChunkSerializers.deserializeChunk(this.db, chunkBuilder, chunkVersion & 0xFF);
        Data2dSerializer.deserialize(this.db, chunkBuilder);

        BlockEntitySerializer.loadBlockEntities(this.db, chunkBuilder);
        EntitySerializer.loadEntities(this.db, chunkBuilder);
        PendingTickSerializer.loadPendingTicks(this.db, chunkBuilder);

        return chunkBuilder.build();
    }

    @Override
    public CompletableFuture<Void> saveChunk(Chunk chunk) {
        checkForClosed();
        int x = chunk.getX();
        int z = chunk.getZ();

        return CompletableFuture.runAsync(() -> {
            if (!chunk.isGenerated()) {
                return;
            }

            try (DirectWriteBatch batch = this.db.createWriteBatch()) {
                Runnable onSuccess;
                long saveRevision;
                CloudChunk cloudChunk = (CloudChunk) chunk;
                try (LockedChunk ignored = cloudChunk.lockForRead()) {
                    ChunkSerializers.serializeChunk(batch, chunk, CURRENT_CHUNK_VERSION);

                    batch.put(LevelDBKey.VERSION.getKey(x, z), new byte[]{(byte) CURRENT_CHUNK_VERSION});

                    int stateValue = cloudChunk.getGenerationStatus().ordinal() - 1;
                    batch.put(LevelDBKey.STATE_FINALIZATION.getKey(x, z), new byte[]{
                            (byte) stateValue,
                            (byte) (stateValue >>> 8),
                            (byte) (stateValue >>> 16),
                            (byte) (stateValue >>> 24)
                    });

                    BlockEntitySerializer.saveBlockEntities(batch, (CloudChunk) chunk);
                    EntitySerializer.saveEntities(batch, (CloudChunk) chunk);
                    onSuccess = PendingTickSerializer.savePendingTicks(batch, (CloudChunk) chunk);
                    saveRevision = cloudChunk.captureSaveRevision();
                }

                writeBatch(batch, "chunk (" + x + ", " + z + ')', () -> {
                    cloudChunk.acknowledgeSave(saveRevision);
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
            } catch (Exception exception) {
                throw asCompletionException("Could not save chunk (" + x + ", " + z + ')', exception);
            }
        }, this.executor);
    }

    @Override
    public CompletableFuture<Void> savePendingTicks(CloudChunk chunk) {
        checkForClosed();
        int x = chunk.getX();
        int z = chunk.getZ();

        return CompletableFuture.runAsync(() -> {
            try (DirectWriteBatch batch = this.db.createWriteBatch()) {
                Runnable onSuccess = PendingTickSerializer.savePendingTicks(batch, chunk);
                if (onSuccess != null) {
                    writeBatch(batch, "pending ticks for chunk (" + x + ", " + z + ')', onSuccess);
                }
            } catch (Exception exception) {
                throw asCompletionException("Could not save pending ticks for chunk (" + x + ", " + z + ')', exception);
            }
        }, this.executor);
    }

    @Override
    public CompletableFuture<Optional<CloudLevelData>> loadLevelData(CloudLevelData initialData) {
        checkForClosed();

        return CompletableFuture.supplyAsync(() -> {
            try {
                return LevelDBDataSerializer.INSTANCE.load(initialData, this.path);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, this.executor);
    }

    @Override
    public CompletableFuture<Void> saveLevelData(CloudLevelData levelData) {
        checkForClosed();
        return CompletableFuture.runAsync(() -> {
            try {
                LevelDBDataSerializer.INSTANCE.save(levelData, path);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, this.executor);
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.db.close();
    }

    private void checkForClosed() {
        Preconditions.checkState(!closed, "LevelProvider closed");
    }

    private void writeBatch(DirectWriteBatch batch, String operation, Runnable onSuccess) {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= SAVE_MAX_ATTEMPTS; attempt++) {
            try {
                this.db.write(batch);
            } catch (Exception exception) {
                lastFailure = exception;
                log.warn("Write attempt {}/{} failed for {}: {}", attempt, SAVE_MAX_ATTEMPTS, operation,
                        exception.getMessage());
                if (attempt < SAVE_MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(50L * attempt);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException("Interrupted while saving " + operation, interruptedException);
                    }
                }
                continue;
            }

            onSuccess.run();
            return;
        }

        throw new CompletionException("Failed to save " + operation + " after " + SAVE_MAX_ATTEMPTS + " attempts", lastFailure);
    }

    private static CompletionException asCompletionException(String message, Exception exception) {
        if (exception instanceof CompletionException completionException) {
            return completionException;
        }

        return new CompletionException(message, exception);
    }
}
