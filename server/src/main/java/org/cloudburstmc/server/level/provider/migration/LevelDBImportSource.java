package org.cloudburstmc.server.level.provider.migration;

import net.daporkchop.ldbjni.LevelDB;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Imports one dimension from an offline LevelDB snapshot using a detected or explicitly selected encoding.
 */
public class LevelDBImportSource implements LevelImportSource {

    private final OfflineLevelDBSnapshot snapshot;
    private final LevelDBImportSource owner;
    private final DB database;
    private final LevelDBChunkReader reader;
    private final LevelDBChunkCodec codec;
    private final Executor executor;
    private final int dimension;

    private boolean closed;

    /**
     * Opens a temporary copy of an offline source for one dimension.
     * A {@code null} profile selects metadata-based detection. The executor handles metadata loading.
     */
    public LevelDBImportSource(Path source, int dimension, @Nullable LevelDBImportProfile profile, Executor executor) throws IOException {
        if (dimension < 0 || dimension > 2) {
            throw new IllegalArgumentException("Unknown import dimension: " + dimension);
        }

        this.executor = Objects.requireNonNull(executor, "executor");
        this.owner = this;
        this.dimension = dimension;
        this.snapshot = new OfflineLevelDBSnapshot(source);
        DB database = null;
        try {
            LevelDBChunkCodec codec = LevelDBImportProfile.detect(readMetadata(this.snapshot.path()), profile).createCodec();
            this.codec = codec;
            database = LevelDB.PROVIDER.open(this.snapshot.path().resolve("db").toFile(), new Options()
                    .createIfMissing(false).compressionType(CompressionType.ZLIB_RAW).maxOpenFiles(64));
            this.database = database;
            this.reader = new LevelDBChunkReader(database, dimension, codec);
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

    private LevelDBImportSource(LevelDBImportSource owner, int dimension) {
        this.owner = owner;
        this.snapshot = owner.snapshot;
        this.database = owner.database;
        this.executor = owner.executor;
        this.dimension = dimension;
        this.codec = owner.codec;
        this.reader = new LevelDBChunkReader(this.database, dimension, this.codec);
    }

    /**
     * Creates a dimension view sharing this import's database and snapshot lifetime.
     * Closing a view does not close the owning source.
     * Closing the owner invalidates all views. Operations on related views are serialized.
     */
    public LevelDBImportSource forDimension(int dimension) {
        if (dimension < 0 || dimension > 2) {
            throw new IllegalArgumentException("Unknown import dimension: " + dimension);
        }

        synchronized (this.owner) {
            checkOpen();
            return new LevelDBImportSource(this.owner, dimension);
        }
    }

    /**
     * Reads level metadata, falling back to its backup without repairing the source files.
     */
    public static NbtMap readMetadata(Path source) throws IOException {
        try {
            return LevelDBDataSerializer.readMetadata(source.resolve("level.dat"));
        } catch (IOException failure) {
            try {
                return LevelDBDataSerializer.readMetadata(source.resolve("level.dat_old"));
            } catch (IOException backupFailure) {
                failure.addSuppressed(backupFailure);
                throw failure;
            }
        }
    }

    @Override
    public CompletableFuture<CloudLevelData> loadLevelData(CloudLevelData initialData) {
        Objects.requireNonNull(initialData, "initialData");
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this.owner) {
                checkOpen();
                try {
                    CloudLevelData data = LevelDBDataSerializer.INSTANCE.load(initialData, this.snapshot.path())
                            .orElseThrow(() -> new IOException("LevelDB level.dat is missing"));
                    data.setDimension(this.dimension);
                    return data;
                } catch (IOException failure) {
                    throw new CompletionException(failure);
                }
            }
        }, this.executor);
    }

    /**
     * Returns dimensions with saved chunk-version records, excluding auxiliary records.
     * An empty database returns an empty set. A nonempty database without chunk records fails.
     */
    public Set<Integer> storedDimensions() throws IOException {
        synchronized (this.owner) {
            checkOpen();
            Set<Integer> dimensions = new TreeSet<>();
            boolean hasRecords = false;
            try (DBIterator iterator = this.database.iterator()) {
                iterator.seekToFirst();
                while (iterator.hasNext()) {
                    hasRecords = true;
                    LevelDBImportKeys.storedChunk(iterator.next().getKey()).ifPresent(chunk -> dimensions.add(chunk.dimension()));
                }
            }

            if (hasRecords && dimensions.isEmpty()) {
                throw new IOException("LevelDB database contains records but no supported chunk keys");
            }

            return Set.copyOf(dimensions);
        }
    }

    /**
     * Visits each chunk in this dimension once, even when both version keys are present.
     * Each imported chunk is closed after the consumer returns while the owner lock is held.
     */
    @Override
    public CompletableFuture<Void> visitChunks(CloudChunkBuilderFactory factory, Consumer<ImportedChunk> consumer) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(consumer, "consumer");
        return CompletableFuture.runAsync(() -> {
            synchronized (this.owner) {
                checkOpen();
                Set<LevelDBImportKeys.ChunkAddress> visited = new HashSet<>();
                Set<Integer> storedDimensions = new TreeSet<>();
                boolean hasRecords = false;
                try (DBIterator iterator = this.database.iterator()) {
                    iterator.seekToFirst();
                    while (iterator.hasNext()) {
                        hasRecords = true;
                        Optional<LevelDBImportKeys.ChunkAddress> address = LevelDBImportKeys.storedChunk(iterator.next().getKey());
                        if (address.isEmpty()) {
                            continue;
                        }

                        LevelDBImportKeys.ChunkAddress chunk = address.get();
                        storedDimensions.add(chunk.dimension());
                        if (chunk.dimension() != this.dimension || !visited.add(chunk)) {
                            continue;
                        }

                        CloudChunkBuilder builder = factory.create(chunk.x(), chunk.z());
                        if (builder.getLevel().getDimension() != this.dimension) {
                            throw new IOException("Import source and destination dimensions do not match");
                        }

                        try (ImportedChunk imported = this.reader.read(builder)) {
                            consumer.accept(imported);
                        } catch (IOException | RuntimeException failure) {
                            throw new IOException("Unable to import LevelDB chunk (" + chunk.x() + ", " + chunk.z() + ")", failure);
                        }
                    }

                    if (visited.isEmpty() && hasRecords) {
                        if (storedDimensions.isEmpty()) {
                            throw new IOException("LevelDB database contains records but no supported chunk keys");
                        }

                        throw new IOException("No chunks for configured dimension " + this.dimension + ". Stored dimensions: " + storedDimensions);
                    }
                } catch (IOException failure) {
                    throw new CompletionException("Unable to import LevelDB dimension " + this.dimension, failure);
                }
            }
        }, Thread::startVirtualThread);
    }

    /**
     * Closes this view, or closes the database and removes its snapshot when called on the owner.
     */
    @Override
    public void close() throws IOException {
        synchronized (this.owner) {
            if (this.closed) {
                return;
            }

            this.closed = true;
            if (this.owner != this) {
                return;
            }

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
        if (this.closed || this.owner.closed) {
            throw new IllegalStateException("Import source is closed");
        }
    }
}
