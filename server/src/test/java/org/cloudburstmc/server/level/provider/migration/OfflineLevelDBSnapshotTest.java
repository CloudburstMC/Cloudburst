package org.cloudburstmc.server.level.provider.migration;

import com.sun.nio.file.ExtendedOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OfflineLevelDBSnapshotTest {

    @Test
    void copiesSourceWithoutOpeningItAsADatabaseAndDeletesOnlyTheCopy(@TempDir Path directory) throws Exception {
        Path database = source(directory);
        Path copied;
        try (OfflineLevelDBSnapshot snapshot = new OfflineLevelDBSnapshot(directory)) {
            copied = snapshot.path();
            assertArrayEquals(Files.readAllBytes(database.resolve("CURRENT")), Files.readAllBytes(copied.resolve("db/CURRENT")));
            Files.writeString(copied.resolve("db/CURRENT"), "changed copy");
            assertEquals("MANIFEST-000001\n", Files.readString(database.resolve("CURRENT")));
        }

        assertFalse(Files.exists(copied));
        assertTrue(Files.isRegularFile(database.resolve("CURRENT")));
        assertEquals("metadata", Files.readString(directory.resolve("level.dat")));
    }

    @Test
    void copiesExportedWorldWithoutCreatingAMissingSourceLock(@TempDir Path directory) throws Exception {
        Path database = source(directory);
        Files.delete(database.resolve("LOCK"));
        Path copied;

        try (OfflineLevelDBSnapshot snapshot = new OfflineLevelDBSnapshot(directory)) {
            copied = snapshot.path();
            assertEquals("MANIFEST-000001\n", Files.readString(copied.resolve("db/CURRENT")));
            assertEquals("manifest", Files.readString(copied.resolve("db/MANIFEST-000001")));
            assertEquals("metadata", Files.readString(copied.resolve("level.dat")));
            assertFalse(Files.exists(copied.resolve("db/LOCK")));
            assertFalse(Files.exists(database.resolve("LOCK")));
        }

        assertFalse(Files.exists(copied));
        assertFalse(Files.exists(database.resolve("LOCK")));
        assertEquals("MANIFEST-000001\n", Files.readString(database.resolve("CURRENT")));
    }

    @Test
    void copiesBackupMetadataWithoutRepairingTheSource(@TempDir Path directory) throws Exception {
        source(directory);
        Files.writeString(directory.resolve("level.dat_old"), "backup metadata");
        Files.delete(directory.resolve("level.dat"));

        try (OfflineLevelDBSnapshot snapshot = new OfflineLevelDBSnapshot(directory)) {
            assertFalse(Files.exists(snapshot.path().resolve("level.dat")));
            assertEquals("backup metadata", Files.readString(snapshot.path().resolve("level.dat_old")));
        }

        assertFalse(Files.exists(directory.resolve("level.dat")));
        assertEquals("backup metadata", Files.readString(directory.resolve("level.dat_old")));
    }

    @Test
    void retainsBothMetadataRecoveryCandidates(@TempDir Path directory) throws Exception {
        source(directory);
        Files.writeString(directory.resolve("level.dat_old"), "backup metadata");

        try (OfflineLevelDBSnapshot snapshot = new OfflineLevelDBSnapshot(directory)) {
            assertEquals("metadata", Files.readString(snapshot.path().resolve("level.dat")));
            assertEquals("backup metadata", Files.readString(snapshot.path().resolve("level.dat_old")));
        }
    }

    @Test
    void rejectsASourceWhoseDatabaseLockIsHeld(@TempDir Path directory) throws Exception {
        Path database = source(directory);
        try (FileChannel channel = FileChannel.open(database.resolve("LOCK"), StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {
            assertTrue(lock.isValid());
            assertThrows(IOException.class, () -> {
                try (OfflineLevelDBSnapshot snapshot = new OfflineLevelDBSnapshot(directory)) {
                    fail("Accepted a locked database and created snapshot at " + snapshot.path());
                }
            });
        }
    }

    @Test
    void waitsForATransientWindowsSharingLockBeforeDeletingTheSnapshot(@TempDir Path directory) throws Exception {
        assumeTrue(File.separatorChar == '\\');
        source(directory);
        Path copied;

        try (OfflineLevelDBSnapshot snapshot = new OfflineLevelDBSnapshot(directory)) {
            copied = snapshot.path();
            try (FileChannel locked = FileChannel.open(copied.resolve("db/MANIFEST-000001"),
                    StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE);
                 ExecutorService executor = Executors.newSingleThreadExecutor()) {
                Future<Void> release = executor.submit(() -> {
                    Thread.sleep(150);
                    locked.close();
                    return null;
                });

                snapshot.close();
                release.get();
            }
        }

        assertFalse(Files.exists(copied));
        assertEquals("metadata", Files.readString(directory.resolve("level.dat")));
    }

    @Test
    void missingSourceDoesNotCreateDirectories(@TempDir Path directory) {
        Path source = directory.resolve("missing");

        assertThrows(IOException.class, () -> {
            try (OfflineLevelDBSnapshot snapshot = new OfflineLevelDBSnapshot(source)) {
                fail("Accepted a missing source and created snapshot at " + snapshot.path());
            }
        });

        assertFalse(Files.exists(source));
    }

    private static Path source(Path directory) throws IOException {
        Path database = Files.createDirectory(directory.resolve("db"));
        Files.writeString(database.resolve("CURRENT"), "MANIFEST-000001\n");
        Files.writeString(database.resolve("MANIFEST-000001"), "manifest");
        Files.write(database.resolve("LOCK"), new byte[0]);
        Files.writeString(directory.resolve("level.dat"), "metadata");
        return database;
    }
}
