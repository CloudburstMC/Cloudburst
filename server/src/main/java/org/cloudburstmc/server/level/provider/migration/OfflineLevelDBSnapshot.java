package org.cloudburstmc.server.level.provider.migration;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Copies an offline LevelDB world so database recovery cannot change the source.
 */
public class OfflineLevelDBSnapshot implements Closeable {

    private final Path path;

    public OfflineLevelDBSnapshot(Path source) throws IOException {
        Path world = source.toRealPath();
        Path database = world.resolve("db");
        if (!Files.isRegularFile(database.resolve("CURRENT"), LinkOption.NOFOLLOW_LINKS)
                || !(Files.isRegularFile(world.resolve("level.dat"), LinkOption.NOFOLLOW_LINKS)
                || Files.isRegularFile(world.resolve("level.dat_old"), LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException("Not a LevelDB world: " + source);
        }

        this.path = Files.createTempDirectory("cloudburst-nukkit-import-");
        try {
            try (FileChannel channel = FileChannel.open(database.resolve("LOCK"), StandardOpenOption.READ);
                 FileLock lock = channel.tryLock(0, Long.MAX_VALUE, true)) {
                if (lock == null) {
                    throw new IOException("Stop the source server before importing " + source);
                }

                List<Path> files = files(database);
                Path target = Files.createDirectory(this.path.resolve("db"));
                for (Path file : files) {
                    copyUnchanged(file, target.resolve(file.getFileName()));
                }

                for (String name : List.of("level.dat", "level.dat_old")) {
                    Path metadata = world.resolve(name);
                    if (Files.exists(metadata, LinkOption.NOFOLLOW_LINKS)) {
                        copyUnchanged(metadata, this.path.resolve(name));
                    }
                }
                if (!files.equals(files(database))) {
                    throw new IOException("Source database changed while being copied");
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

    public Path path() {
        return this.path;
    }

    @Override
    public void close() throws IOException {
        if (!Files.exists(this.path)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(this.path)) {
            for (Path file : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
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

    private static void copyUnchanged(Path source, Path target) throws IOException {
        BasicFileAttributes before = Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile()) {
            throw new IOException("Import source must contain only regular files: " + source);
        }

        Files.copy(source, target);
        BasicFileAttributes after = Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !Objects.equals(before.fileKey(), after.fileKey())) {
            throw new IOException("Import source changed while being copied: " + source);
        }
    }
}
