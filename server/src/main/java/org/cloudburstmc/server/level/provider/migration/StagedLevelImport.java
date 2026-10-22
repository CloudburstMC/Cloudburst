package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.server.level.CloudLevelLoadRequest;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Stages conversion and retains the original level when publishing its replacement.
 */
public class StagedLevelImport implements Closeable {

    private final Path stagingRoot;
    private final Path destination;
    private final Path backup;

    public StagedLevelImport(Path levelsPath, String destinationId, Path source) throws IOException {
        CloudLevelLoadRequest.validateId(destinationId);
        Path world = source.toRealPath();
        Path root = levelsPath.toRealPath();
        this.destination = root.resolve(destinationId).normalize();

        if (!root.equals(this.destination.getParent()) || !this.destination.equals(world) || Files.isSymbolicLink(this.destination)) {
            throw new IOException("Automatic import requires a level directory directly inside " + root);
        }

        this.backup = root.resolve("." + destinationId + ".nukkit-backup");
        if (Files.exists(this.backup, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Original level backup already exists: " + this.backup);
        }

        this.stagingRoot = Files.createTempDirectory(root, ".import-");
    }

    public Path stagingRoot() {
        return this.stagingRoot;
    }

    public Path publish() throws IOException {
        Path stagedLevel = this.stagingRoot.resolve(this.destination.getFileName());
        if (!Files.isDirectory(stagedLevel, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Import produced no destination level");
        }

        Files.move(this.destination, this.backup);
        try {
            Files.move(stagedLevel, this.destination);
        } catch (IOException failure) {
            try {
                Files.move(this.backup, this.destination);
            } catch (IOException restoreFailure) {
                failure.addSuppressed(restoreFailure);
            }
            throw failure;
        }

        return this.backup;
    }

    @Override
    public void close() throws IOException {
        try (Stream<Path> paths = Files.walk(this.stagingRoot)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
