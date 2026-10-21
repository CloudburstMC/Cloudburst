package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.provider.LevelImportFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LevelImportFormatTest {

    @Test
    void detectsNukkitMetadataWithoutOpeningDatabase(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder()
                .putString("generatorName", "normal")
                .putString("generatorOptions", "")
                .build());

        assertEquals(LevelImportFormat.NUKKIT_LEVELDB, LevelImportFormat.detect(directory));
    }

    @Test
    void nativeWorldDoesNotRequireRecentClockMetadata(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder()
                .putString("generatorName", "cloudburst:standard")
                .putString("generatorOptions", "")
                .build());

        assertNull(LevelImportFormat.detect(directory));
    }

    @Test
    void detectsAnvilRegions(@TempDir Path directory) throws Exception {
        Files.createDirectory(directory.resolve("region"));
        Files.createFile(directory.resolve("region/r.0.0.mca"));

        assertEquals(LevelImportFormat.NUKKIT_ANVIL, LevelImportFormat.detect(directory));
    }

    @Test
    void interruptedPublicationCannotCreateEmptyWorld(@TempDir Path directory) throws Exception {
        Files.createDirectory(directory.resolve(".world.nukkit-backup"));

        assertThrows(IOException.class, () -> LevelImportFormat.detect(directory.resolve("world")));
        assertFalse(Files.exists(directory.resolve("world")));
    }

    private static void writeMetadata(Path directory, NbtMap tag) throws IOException {
        Files.createDirectories(directory.resolve("db"));
        byte[] payload;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            try (NBTOutputStream writer = NbtUtils.createWriterLE(bytes)) {
                writer.writeTag(tag);
            }
            payload = bytes.toByteArray();
        }

        byte[] metadata = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(10).putInt(payload.length).put(payload).array();
        Files.write(directory.resolve("level.dat"), metadata);
    }
}
