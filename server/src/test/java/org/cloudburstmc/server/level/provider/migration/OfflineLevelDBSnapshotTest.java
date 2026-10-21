package org.cloudburstmc.server.level.provider.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.*;

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
