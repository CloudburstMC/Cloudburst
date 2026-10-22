package org.cloudburstmc.server.level.provider.migration;

import net.daporkchop.ldbjni.LevelDB;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * Copies an offline LevelDB world so database recovery cannot change the source.
 * The source must remain offline. Lock and file-attribute checks detect changes but do not
 * provide a transactional snapshot of a running database.
 */
public class OfflineLevelDBSnapshot implements Closeable {

    private static final int CLEANUP_ATTEMPTS = 20;
    private static final long CLEANUP_RETRY_MILLIS = 50;

    private final Path path;

    public OfflineLevelDBSnapshot(Path source) throws IOException {
        Path world = source.toRealPath();
        Path database = world.resolve("db");
        if (!Files.isRegularFile(database.resolve("CURRENT"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Not a LevelDB world: " + source);
        }

        this.path = Files.createTempDirectory("cloudburst-leveldb-import-");
        try {
            try (FileChannel channel = openSourceLock(database.resolve("LOCK"));
                 FileLock lock = channel == null ? null : channel.tryLock(0, Long.MAX_VALUE, true)) {
                if (channel != null && lock == null) {
                    throw new IOException("Stop the source server before importing " + source);
                }

                List<Path> files = files(database);
                Map<Path, BasicFileAttributes> original = new LinkedHashMap<>();
                for (Path file : files) {
                    original.put(file, attributes(file));
                }

                for (String name : List.of("level.dat", "level.dat_old")) {
                    Path metadata = world.resolve(name);
                    if (Files.exists(metadata, LinkOption.NOFOLLOW_LINKS)) {
                        original.put(metadata, attributes(metadata));
                    }
                }

                Files.createDirectory(this.path.resolve("db"));
                for (Path file : original.keySet()) {
                    Files.copy(file, this.path.resolve(world.relativize(file)), LinkOption.NOFOLLOW_LINKS);
                }

                if (!files.equals(files(database))) {
                    throw new IOException("Source database changed while being copied");
                }

                for (String name : List.of("level.dat", "level.dat_old")) {
                    Path metadata = world.resolve(name);
                    if (original.containsKey(metadata) != Files.exists(metadata, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("Source metadata changed while being copied: " + metadata);
                    }
                }

                for (Map.Entry<Path, BasicFileAttributes> entry : original.entrySet()) {
                    requireUnchanged(entry.getKey(), entry.getValue(), attributes(entry.getKey()));
                }
            } catch (OverlappingFileLockException failure) {
                throw new IOException("Stop the source server before importing " + source, failure);
            }
        } catch (IOException | RuntimeException failure) {
            try {
                close();
            } catch (IOException cleanup) {
                failure.addSuppressed(cleanup);
            }

            throw failure;
        }
    }

    /**
     * Returns the temporary world directory owned by this snapshot.
     */
    public Path path() {
        return this.path;
    }

    /**
     * Checks for database records using a temporary copy. An absent or empty directory is empty.
     * Invalid databases fail rather than being treated as new worlds.
     */
    public static boolean isDatabaseEmpty(Path source) throws IOException {
        Path database = source.resolve("db");
        if (Files.notExists(database, LinkOption.NOFOLLOW_LINKS)) {
            return true;
        }

        if (!Files.isDirectory(database, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Not a LevelDB directory: " + database);
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(database)) {
            if (!files.iterator().hasNext()) {
                return true;
            }
        }

        try (OfflineLevelDBSnapshot snapshot = new OfflineLevelDBSnapshot(source);
             DB copy = LevelDB.PROVIDER.open(snapshot.path().resolve("db").toFile(), new Options()
                     .createIfMissing(false).compressionType(CompressionType.ZLIB_RAW).maxOpenFiles(64));
             DBIterator iterator = copy.iterator()) {
            iterator.seekToFirst();
            return !iterator.hasNext();
        } catch (RuntimeException failure) {
            throw new IOException("Unable to inspect LevelDB database: " + database, failure);
        }
    }

    /**
     * Deletes the temporary copy. Database handles opened within it must be closed first.
     */
    @Override
    public void close() throws IOException {
        if (!Files.exists(this.path)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(this.path)) {
            for (Path file : paths.sorted(Comparator.reverseOrder()).toList()) {
                deleteSnapshotFile(file);
            }
        }
    }

    // Windows can retain mapped LevelDB tables after close, so deletion uses bounded, interruptible retries.
    @SuppressWarnings("BusyWait")
    private static void deleteSnapshotFile(Path file) throws IOException {
        for (int attempt = 1; ; attempt++) {
            try {
                Files.deleteIfExists(file);
                return;
            } catch (FileSystemException failure) {
                if (File.separatorChar != '\\' || attempt >= CLEANUP_ATTEMPTS) {
                    throw failure;
                }

                try {
                    Thread.sleep(CLEANUP_RETRY_MILLIS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    InterruptedIOException cleanup = new InterruptedIOException("Interrupted while deleting import snapshot: " + file);
                    cleanup.initCause(interrupted);
                    cleanup.addSuppressed(failure);
                    throw cleanup;
                }
            }
        }
    }

    private static @Nullable FileChannel openSourceLock(Path lock) throws IOException {
        try {
            return FileChannel.open(lock, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException missing) {
            return null;
        }
    }

    private static List<Path> files(Path directory) throws IOException {
        if (Files.isSymbolicLink(directory)) {
            throw new IOException("Database directory must not be a symbolic link: " + directory);
        }

        try (Stream<Path> files = Files.list(directory)) {
            return files.sorted().toList();
        }
    }

    private static BasicFileAttributes attributes(Path source) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile()) {
            throw new IOException("Import source must contain only regular files: " + source);
        }
        return attributes;
    }

    private static void requireUnchanged(Path source, BasicFileAttributes before, BasicFileAttributes after) throws IOException {
        if (before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime()) || !Objects.equals(before.fileKey(), after.fileKey())) {
            throw new IOException("Import source changed while being copied: " + source);
        }
    }
}
