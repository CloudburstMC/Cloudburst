package org.cloudburstmc.server.level.provider.leveldb;

import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;
import net.daporkchop.ldbjni.LevelDB;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.ldbjni.direct.DirectWriteBatch;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.chunk.LockableChunk;
import org.cloudburstmc.server.level.LevelData;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.LevelProvider;
import org.cloudburstmc.server.level.provider.leveldb.serializer.*;
import org.cloudburstmc.server.utils.LoadState;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

@Log4j2
@ParametersAreNonnullByDefault
class LevelDBProvider implements LevelProvider {
    private static final int CURRENT_CHUNK_VERSION = 42;

    private final String levelId;
    private final Path path;
    private final Executor executor;
    private final DirectDB db;
    private volatile boolean closed;

    LevelDBProvider(String levelId, Path worldPath, Executor executor) throws IOException {
        this.levelId = levelId;
        this.path = worldPath.resolve(levelId);
        this.executor = executor;
        Path dbPath = this.path.resolve("db");
        Files.createDirectories(dbPath);
        Preconditions.checkArgument(Files.isDirectory(dbPath), "db is not a directory");

        Options options = new Options()
                .createIfMissing(true)
                .compressionType(CompressionType.ZLIB_RAW)
                .blockSize(64 * 1024);
        this.db = LevelDB.PROVIDER.open(dbPath.toFile(), options);
    }

    @Override
    public String getLevelId() {
        return levelId;
    }

    @Override
    public CompletableFuture<CloudChunk> readChunk(ChunkBuilder chunkBuilder) {
        final int x = chunkBuilder.getX();
        final int z = chunkBuilder.getZ();

        return CompletableFuture.supplyAsync(() -> {
            byte[] versionValue = this.db.get(LevelDBKey.VERSION.getKey(x, z));
            if (versionValue == null || versionValue.length != 1) {
                versionValue = this.db.get(LevelDBKey.VERSION_OLD.getKey(x, z));
            }

            if (versionValue == null || versionValue.length != 1) {
                return null;
            }

            byte[] finalizationState = this.db.get(LevelDBKey.STATE_FINALIZATION.getKey(x, z));
            if (finalizationState == null) {
                chunkBuilder.state(Chunk.STATE_FINISHED);
            } else {
                int stateValue = (finalizationState[0] & 0xFF)
                        | ((finalizationState[1] & 0xFF) << 8)
                        | ((finalizationState[2] & 0xFF) << 16)
                        | ((finalizationState[3] & 0xFF) << 24);
                chunkBuilder.state(stateValue + 1);
            }

            byte chunkVersion = versionValue[0];

            if (chunkVersion < 7) {
                chunkBuilder.dirty();
            }

            chunkBuilder.chunkVersion(chunkVersion & 0xFF);
            ChunkSerializers.deserializeChunk(this.db, chunkBuilder, chunkVersion & 0xFF);
            Data2dSerializer.deserialize(this.db, chunkBuilder);

            BlockEntitySerializer.loadBlockEntities(this.db, chunkBuilder);
            EntitySerializer.loadEntities(this.db, chunkBuilder);

            return chunkBuilder.build();
        }, this.executor);
    }

    @Override
    public CompletableFuture<Void> saveChunk(Chunk chunk) {
        final int x = chunk.getX();
        final int z = chunk.getZ();

        return CompletableFuture.supplyAsync(() -> {
            // Clear the dirty flag here rather than in LevelChunkManager, in case the chunk
            // is modified between when it was enqueued and when it is actually written.
            if (!chunk.isGenerated() || !chunk.clearDirty()) {
                return null;
            }

            try (DirectWriteBatch batch = this.db.createWriteBatch()) {
                LockableChunk lockableChunk = chunk.readLockable();
                lockableChunk.lock();
                try {
                    ChunkSerializers.serializeChunk(batch, chunk, CURRENT_CHUNK_VERSION);

                    batch.put(LevelDBKey.VERSION.getKey(x, z), new byte[]{(byte) CURRENT_CHUNK_VERSION});

                    int stateValue = lockableChunk.getState() - 1;
                    batch.put(LevelDBKey.STATE_FINALIZATION.getKey(x, z), new byte[]{
                            (byte) stateValue,
                            (byte) (stateValue >>> 8),
                            (byte) (stateValue >>> 16),
                            (byte) (stateValue >>> 24)
                    });

                    BlockEntitySerializer.saveBlockEntities(batch, (CloudChunk) chunk);
                    EntitySerializer.saveEntities(batch, (CloudChunk) chunk);
                } finally {
                    lockableChunk.unlock();
                }

                this.db.write(batch);
                return null;
            } catch (IOException e) {
                // can't happen
                throw new RuntimeException(e);
            }
        }, this.executor);
    }

    @Override
    public CompletableFuture<Void> forEachChunk(ChunkBuilder.Factory factory, BiConsumer<CloudChunk, Throwable> consumer) {
        // TODO: implement chunk iteration
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<LoadState> loadLevelData(LevelData levelData) {
        checkForClosed();

        return CompletableFuture.supplyAsync(() -> {
            try {
                return LevelDBDataSerializer.INSTANCE.load(levelData, path, levelId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, this.executor);
    }

    @Override
    public CompletableFuture<Void> saveLevelData(LevelData levelData) {
        checkForClosed();
        return CompletableFuture.runAsync(() -> {
            try {
                LevelDBDataSerializer.INSTANCE.save(levelData, path, levelId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, this.executor).exceptionally((e) -> {
            log.catching(e);
            return null;
        });
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.db.close();
    }

    private void checkForClosed() {
        Preconditions.checkState(!closed, "LevelProvider closed");
    }
}
