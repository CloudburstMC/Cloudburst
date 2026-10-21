package org.cloudburstmc.server.level.provider.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StagedLevelImportTest {

    @Test
    void publishesConvertedWorldAndRetainsOriginal(@TempDir Path directory) throws Exception {
        Path source = Files.createDirectory(directory.resolve("world"));
        Files.writeString(source.resolve("original"), "original data");
        Path stagingRoot;

        try (StagedLevelImport staging = new StagedLevelImport(directory, "world", source)) {
            stagingRoot = staging.stagingRoot();
            Path converted = Files.createDirectory(stagingRoot.resolve("world"));
            Files.writeString(converted.resolve("converted"), "converted data");
            staging.publish();
        }

        assertEquals("converted data", Files.readString(source.resolve("converted")));
        assertEquals("original data", Files.readString(directory.resolve(".world.nukkit-backup/original")));
        assertFalse(Files.exists(stagingRoot));
    }

    @Test
    void failedConversionLeavesOriginalUntouched(@TempDir Path directory) throws Exception {
        Path source = Files.createDirectory(directory.resolve("world"));
        Files.writeString(source.resolve("original"), "original data");
        Path stagingRoot;

        try (StagedLevelImport staging = new StagedLevelImport(directory, "world", source)) {
            stagingRoot = staging.stagingRoot();
            assertThrows(IOException.class, staging::publish);
        }

        assertEquals("original data", Files.readString(source.resolve("original")));
        assertFalse(Files.exists(directory.resolve(".world.nukkit-backup")));
        assertFalse(Files.exists(stagingRoot));
    }

    @Test
    void neverOverwritesExistingBackup(@TempDir Path directory) throws Exception {
        Path source = Files.createDirectory(directory.resolve("world"));
        Path backup = Files.createDirectory(directory.resolve(".world.nukkit-backup"));
        Files.writeString(backup.resolve("original"), "previous backup");

        assertThrows(IOException.class, () -> {
            try (StagedLevelImport staging = new StagedLevelImport(directory, "world", source)) {
                fail("Accepted an existing backup and created staging at " + staging.stagingRoot());
            }
        });

        assertEquals("previous backup", Files.readString(backup.resolve("original")));
        assertTrue(Files.isDirectory(source));
    }
}
