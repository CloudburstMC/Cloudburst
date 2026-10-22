package org.cloudburstmc.server.level.provider;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.server.level.provider.anvil.AnvilLevelImportSource;
import org.cloudburstmc.server.level.provider.migration.LevelDBImportProfile;
import org.cloudburstmc.server.level.provider.migration.LevelDBImportSource;
import org.cloudburstmc.server.level.provider.migration.OfflineLevelDBSnapshot;
import org.cloudburstmc.server.level.provider.migration.StagedLevelImport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.Executor;

/**
 * Source formats converted before a level is opened for normal use.
 */
public enum LevelImportFormat {
    LEVELDB("LevelDB"),
    ANVIL("Anvil");

    private final String displayName;

    LevelImportFormat(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Detects a supported import source and rejects ambiguous or interrupted imports.
     * Returns {@code null} for native storage or when no import format is detected.
     * Source files are not modified or repaired during detection.
     *
     * @param source  directory containing the saved world
     * @param profile explicit LevelDB encoding, or {@code null} for metadata detection
     * @throws IOException if the source is ambiguous, incomplete or uses an unsupported encoding
     */
    public static @Nullable LevelImportFormat detect(Path source, @Nullable LevelDBImportProfile profile) throws IOException {
        StagedLevelImport.checkInterruptedImport(source);

        boolean anvil = AnvilLevelImportSource.isImportSource(source);
        boolean levelDB = Files.isDirectory(source.resolve("db"));
        if (anvil && levelDB) {
            throw new IOException("Level contains both Anvil regions and a LevelDB database: " + source);
        }

        if (anvil) {
            if (profile != null) {
                throw new IOException("import-profile applies only to LevelDB sources");
            }

            return ANVIL;
        }

        if (!levelDB) {
            return null;
        }

        if (Files.notExists(source.resolve("level.dat"), LinkOption.NOFOLLOW_LINKS)
                && Files.notExists(source.resolve("level.dat_old"), LinkOption.NOFOLLOW_LINKS)) {
            if (OfflineLevelDBSnapshot.isDatabaseEmpty(source)) {
                return null;
            }

            throw new IOException("LevelDB database contains records but level.dat and level.dat_old are missing: " + source);
        }

        return LevelDBImportProfile.detect(LevelDBImportSource.readMetadata(source), profile) == LevelDBImportProfile.CLOUDBURST ? null : LEVELDB;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Opens an offline source owned by the caller. The dimension selects LevelDB records only.
     * Keep the source open until its asynchronous operations finish, then close it.
     *
     * @param source    directory containing the saved world
     * @param dimension dimension to read from a LevelDB source
     * @param profile   explicit LevelDB encoding, or {@code null} for metadata detection
     * @param executor  executor for source metadata loading
     */
    public LevelImportSource open(Path source, int dimension, @Nullable LevelDBImportProfile profile, Executor executor) throws IOException {
        return switch (this) {
            case LEVELDB -> new LevelDBImportSource(source, dimension, profile, executor);
            case ANVIL -> new AnvilLevelImportSource(source, executor);
        };
    }
}
