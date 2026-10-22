package org.cloudburstmc.server.level.provider.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
            assertEquals(directory.toRealPath().resolve(".world.import-backup"), staging.publish());
        }

        assertEquals("converted data", Files.readString(source.resolve("converted")));
        assertEquals("original data", Files.readString(directory.resolve(".world.import-backup/original")));
        assertFalse(Files.exists(stagingRoot));
    }

    @Test
    void publishesAllDimensionsAndKeepsOneCompleteSourceBackup(@TempDir Path directory) throws Exception {
        Path source = Files.createDirectory(directory.resolve("world"));
        Files.writeString(source.resolve("original"), "all dimensions");

        try (StagedLevelImport staging = new StagedLevelImport(directory, List.of("world", "nether", "the_end"), source)) {
            for (String id : List.of("world", "nether", "the_end")) {
                Path dimension = Files.createDirectory(staging.stagingRoot().resolve(id));
                Files.writeString(dimension.resolve("converted"), id);
            }

            assertEquals(directory.toRealPath().resolve(".world.import-backup"), staging.publish());
        }

        for (String id : List.of("world", "nether", "the_end")) {
            assertEquals(id, Files.readString(directory.resolve(id).resolve("converted")));
        }

        assertEquals("all dimensions", Files.readString(directory.resolve(".world.import-backup/original")));
    }

    @Test
    void incompleteDimensionConversionDoesNotPublishAnyLevel(@TempDir Path directory) throws Exception {
        Path source = Files.createDirectory(directory.resolve("world"));
        Files.writeString(source.resolve("original"), "all dimensions");

        try (StagedLevelImport staging = new StagedLevelImport(directory, List.of("world", "nether", "the_end"), source)) {
            Files.createDirectory(staging.stagingRoot().resolve("world"));
            Files.createDirectory(staging.stagingRoot().resolve("nether"));

            assertThrows(IOException.class, staging::publish);
        }

        assertEquals("all dimensions", Files.readString(source.resolve("original")));
        assertFalse(Files.exists(directory.resolve("nether")));
        assertFalse(Files.exists(directory.resolve("the_end")));
        assertFalse(Files.exists(directory.resolve(".world.import-backup")));
    }

    @Test
    void neverReplacesExistingCompanionLevels(@TempDir Path directory) throws Exception {
        Path source = Files.createDirectory(directory.resolve("world"));
        Path nether = Files.createDirectory(directory.resolve("nether"));
        Files.writeString(nether.resolve("original"), "existing nether");

        assertThrows(IOException.class, () -> {
            try (StagedLevelImport staging = new StagedLevelImport(directory, List.of("world", "nether"), source)) {
                fail("Accepted an existing companion level at " + staging.stagingRoot());
            }
        });

        assertEquals("existing nether", Files.readString(nether.resolve("original")));
        assertFalse(Files.exists(directory.resolve(".world.import-backup")));
    }

    @Test
    void publicationRechecksCompanionDestinations(@TempDir Path directory) throws Exception {
        Path source = Files.createDirectory(directory.resolve("world"));
        Files.writeString(source.resolve("original"), "all dimensions");

        try (StagedLevelImport staging = new StagedLevelImport(directory, List.of("world", "nether"), source)) {
            Files.createDirectory(staging.stagingRoot().resolve("world"));
            Files.createDirectory(staging.stagingRoot().resolve("nether"));
            Files.createDirectory(directory.resolve("nether"));

            assertThrows(IOException.class, staging::publish);
        }

        assertEquals("all dimensions", Files.readString(source.resolve("original")));
        assertFalse(Files.exists(directory.resolve(".world.import-backup")));
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
        assertFalse(Files.exists(directory.resolve(".world.import-backup")));
        assertFalse(Files.exists(stagingRoot));
    }

    @Test
    void neverOverwritesExistingBackup(@TempDir Path directory) throws Exception {
        Path source = Files.createDirectory(directory.resolve("world"));
        Path backup = Files.createDirectory(directory.resolve(".world.import-backup"));
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
