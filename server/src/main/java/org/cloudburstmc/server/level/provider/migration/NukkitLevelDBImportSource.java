package org.cloudburstmc.server.level.provider.migration;

import net.daporkchop.ldbjni.LevelDB;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilderFactory;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.cloudburstmc.server.level.provider.LevelImportSource;
import org.cloudburstmc.server.level.provider.leveldb.serializer.LevelDBDataSerializer;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Imports one dimension from an offline Nukkit LevelDB world without opening the source database.
 */
public class NukkitLevelDBImportSource implements LevelImportSource {

    private final OfflineLevelDBSnapshot snapshot;
    private final DB database;
    private final NukkitLevelDBChunkReader reader;
    private final int dimension;
    private final Executor executor;
    private boolean closed;

    public NukkitLevelDBImportSource(Path source, int dimension, Executor executor) throws IOException {
        if (dimension < 0 || dimension > 2) {
            throw new IllegalArgumentException("Unknown Nukkit dimension: " + dimension);
        }

        this.executor = Objects.requireNonNull(executor, "executor");
        this.dimension = dimension;
        NukkitBlockStateConverter blocks = new NukkitBlockStateConverter();
        NukkitItemDataConverter items = new NukkitItemDataConverter();
        this.snapshot = new OfflineLevelDBSnapshot(source);
        DB database = null;
        try {
            database = LevelDB.PROVIDER.open(this.snapshot.path().resolve("db").toFile(), new Options()
                    .createIfMissing(false).compressionType(CompressionType.ZLIB_RAW).maxOpenFiles(64));
            this.database = database;
            this.reader = new NukkitLevelDBChunkReader(database, dimension, blocks, items);
        } catch (IOException | RuntimeException failure) {
            if (database != null) {
                try {
                    database.close();
                } catch (IOException cleanup) {
                    failure.addSuppressed(cleanup);
                }
            }

            try {
                this.snapshot.close();
            } catch (IOException cleanup) {
                failure.addSuppressed(cleanup);
            }

            throw failure;
        }
    }

    @Override
    public CompletableFuture<CloudLevelData> loadLevelData(CloudLevelData initialData) {
        Objects.requireNonNull(initialData, "initialData");
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                checkOpen();
                try {
                    CloudLevelData data = LevelDBDataSerializer.INSTANCE.load(initialData, this.snapshot.path())
                            .orElseThrow(() -> new IOException("Nukkit level.dat is missing"));
                    data.setDimension(this.dimension);
                    return data;
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            }
        }, this.executor);
    }

    @Override
    public CompletableFuture<Void> visitChunks(CloudChunkBuilderFactory factory, Consumer<ImportedChunk> consumer) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(consumer, "consumer");
        return CompletableFuture.runAsync(() -> {
            synchronized (this) {
                checkOpen();
                Set<NukkitLevelDBKeys.ChunkAddress> visited = new HashSet<>();
                try (DBIterator iterator = this.database.iterator()) {
                    iterator.seekToFirst();
                    while (iterator.hasNext()) {
                        Optional<NukkitLevelDBKeys.ChunkAddress> address = NukkitLevelDBKeys.chunk(iterator.next().getKey());
                        if (address.isEmpty() || address.get().dimension() != this.dimension || !visited.add(address.get())) {
                            continue;
                        }

                        NukkitLevelDBKeys.ChunkAddress chunk = address.get();
                        CloudChunkBuilder builder = factory.create(chunk.x(), chunk.z());
                        if (builder.getLevel().getDimension() != this.dimension) {
                            throw new IOException("Import source and destination dimensions do not match");
                        }

                        try (ImportedChunk imported = this.reader.read(builder)) {
                            consumer.accept(imported);
                        }
                    }
                } catch (IOException failure) {
                    throw new CompletionException("Unable to import Nukkit LevelDB dimension " + this.dimension, failure);
                }
            }
        }, Thread::startVirtualThread);
    }

    @Override
    public synchronized void close() throws IOException {
        if (!this.closed) {
            this.closed = true;
            try {
                this.database.close();
            } catch (IOException failure) {
                try {
                    this.snapshot.close();
                } catch (IOException cleanup) {
                    failure.addSuppressed(cleanup);
                }

                throw failure;
            }

            this.snapshot.close();
        }
    }

    private void checkOpen() {
        if (this.closed) {
            throw new IllegalStateException("Import source is closed");
        }
    }
}
