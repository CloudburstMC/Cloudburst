package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.server.level.CloudLevelLoadRequest;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stages one or more destination levels and retains the complete source when publishing.
 */
public class StagedLevelImport implements Closeable {

    private final Path stagingRoot;
    private final Path destination;
    private final Path backup;
    private final List<Path> destinations;

    /**
     * Prepares staging to replace a source directory directly inside {@code levelsPath}.
     */
    public StagedLevelImport(Path levelsPath, String destinationId, Path source) throws IOException {
        this(levelsPath, List.of(destinationId), source);
    }

    /**
     * Prepares destinations for an offline import. The first ID must name the source directory
     * directly inside {@code levelsPath}. Other destination directories must not exist.
     * Existing source backups are never replaced.
     */
    public StagedLevelImport(Path levelsPath, List<String> destinationIds, Path source) throws IOException {
        if (destinationIds.isEmpty() || new HashSet<>(destinationIds).size() != destinationIds.size()) {
            throw new IllegalArgumentException("Import destinations must be nonempty and unique");
        }

        destinationIds.forEach(CloudLevelLoadRequest::validateId);
        String destinationId = destinationIds.getFirst();
        Path world = source.toRealPath();
        Path root = levelsPath.toRealPath();
        this.destination = root.resolve(destinationId).normalize();

        if (!root.equals(this.destination.getParent()) || !this.destination.equals(world) || Files.isSymbolicLink(this.destination)) {
            throw new IOException("Automatic import requires a level directory directly inside " + root);
        }

        this.backup = root.resolve("." + destinationId + ".import-backup");
        for (String suffix : new String[]{".import-backup", ".nukkit-backup"}) {
            Path existing = root.resolve("." + destinationId + suffix);
            if (Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Original level backup already exists: " + existing);
            }
        }

        List<Path> destinations = new ArrayList<>();
        for (String id : destinationIds.subList(1, destinationIds.size())) {
            Path destination = root.resolve(id);
            checkInterruptedImport(destination);
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Import destination already exists: " + id + ". Move it aside before importing the combined world");
            }

            destinations.add(destination);
        }

        // Publish the primary last so its absence identifies an interrupted group import.
        destinations.add(this.destination);
        this.destinations = List.copyOf(destinations);
        this.stagingRoot = Files.createTempDirectory(root, ".import-");
    }

    /**
     * Rejects a missing level directory when an import backup indicates an interrupted publication.
     */
    public static void checkInterruptedImport(Path source) throws IOException {
        if (Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        for (String suffix : new String[]{".import-backup", ".nukkit-backup"}) {
            Path backup = source.resolveSibling("." + source.getFileName() + suffix);
            if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Interrupted level conversion. Restore " + backup.getFileName() + " to " + source.getFileName() + " before loading");
            }
        }
    }

    /**
     * Returns the temporary parent directory in which destination levels must be written.
     */
    public Path stagingRoot() {
        return this.stagingRoot;
    }

    /**
     * Publishes all completed destinations and retains the complete source as a backup.
     * Publication uses separate directory moves and is not atomic across destinations.
     * If a move fails, attempts to restore the previous directory layout.
     *
     * @return the retained source backup directory
     */
    public Path publish() throws IOException {
        for (Path destination : this.destinations) {
            if (!Files.isDirectory(this.stagingRoot.resolve(destination.getFileName()), LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Import produced no destination level: " + destination.getFileName());
            }

            if (!destination.equals(this.destination) && Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Import destination already exists: " + destination.getFileName());
            }
        }

        Files.move(this.destination, this.backup);
        List<Path> published = new ArrayList<>();
        try {
            for (Path destination : this.destinations) {
                Files.move(this.stagingRoot.resolve(destination.getFileName()), destination);
                published.add(destination);
            }
        } catch (IOException failure) {
            boolean restored = true;
            for (Path destination : published.reversed()) {
                try {
                    Files.move(destination, this.stagingRoot.resolve(destination.getFileName()));
                } catch (IOException restoreFailure) {
                    restored = false;
                    failure.addSuppressed(restoreFailure);
                }
            }

            if (restored) {
                try {
                    Files.move(this.backup, this.destination);
                } catch (IOException restoreFailure) {
                    failure.addSuppressed(restoreFailure);
                }
            }
            throw failure;
        }

        return this.backup;
    }

    /**
     * Removes remaining staging files without deleting published levels or the source backup.
     */
    @Override
    public void close() throws IOException {
        try (Stream<Path> paths = Files.walk(this.stagingRoot)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
