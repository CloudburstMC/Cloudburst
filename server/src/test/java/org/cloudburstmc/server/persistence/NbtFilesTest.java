package org.cloudburstmc.server.persistence;

import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class NbtFilesTest {

    @TempDir
    Path directory;

    @Test
    void compressedCompoundRoundTripsThroughACompleteGzipFile() throws Exception {
        NbtMap record = NbtMap.builder().putString("name", "example").putLong("value", 42).build();
        Path file = directory.resolve("record.dat");
        Files.write(file, NbtFiles.encodeCompressed(record));

        assertEquals(record, NbtFiles.readCompressed(file));
    }

    @Test
    void scalarRootsAreRejectedAsInvalidRecords() throws Exception {
        Path file = directory.resolve("scalar.dat");
        try (OutputStream bytes = Files.newOutputStream(file);
             NBTOutputStream output = NbtUtils.createGZIPWriter(bytes)) {
            output.writeTag("not a compound");
        }

        assertThrows(IOException.class, () -> NbtFiles.readCompressed(file));

        Files.delete(file);
    }

    @Test
    void invalidGzipHeaderDoesNotLeaveTheFileOpen() throws Exception {
        Path file = directory.resolve("corrupt.dat");
        Files.writeString(file, "not gzip data");

        assertThrows(IOException.class, () -> NbtFiles.readCompressed(file));

        Path moved = directory.resolve("preserved.dat");
        Files.move(file, moved);
        assertEquals("not gzip data", Files.readString(moved));
        Files.delete(moved);
    }

    @Test
    void invalidNbtPayloadDoesNotLeaveTheFileOpen() throws Exception {
        Path file = directory.resolve("corrupt.dat");
        try (OutputStream bytes = Files.newOutputStream(file);
             GZIPOutputStream compressed = new GZIPOutputStream(bytes)) {
            compressed.write(10);
        }

        assertThrows(IOException.class, () -> NbtFiles.readCompressed(file));

        Path moved = directory.resolve("preserved.dat");
        Files.move(file, moved);
        Files.delete(moved);
        assertFalse(Files.exists(moved));
    }
}
