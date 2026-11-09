package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.level.provider.LevelImportFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LevelImportFormatTest {

    @Test
    void detectsLevelDBWithoutOpeningDatabase(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder()
                .putInt("PM1EGen", 2)
                .build());

        assertEquals(LevelImportFormat.LEVELDB, LevelImportFormat.detect(directory, null));
    }

    @Test
    void bedrockVersionMetadataIsNotOverriddenByLegacyGeneratorFields(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder()
                .putString("generatorName", "default").putString("generatorOptions", "")
                .putInt("StorageVersion", 8)
                .putList("lastOpenedWithVersion", NbtType.INT, List.of(1, 21, 100, 23, 0))
                .build());

        assertEquals(LevelImportFormat.LEVELDB, LevelImportFormat.detect(directory, null));
    }

    @Test
    void explicitNukkitMarkerStillIdentifiesImportedBedrockMetadata(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder()
                .putString("generatorName", "normal").putString("generatorOptions", "")
                .putInt("PM1EGen", 2)
                .putList("lastOpenedWithVersion", NbtType.INT, List.of(1, 21, 100, 23, 0))
                .build());

        assertEquals(LevelImportFormat.LEVELDB, LevelImportFormat.detect(directory, null));
    }

    @Test
    void nativeMetadataCanRetainTheOriginalNukkitMarker(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder().putInt("CloudburstStorageVersion", 1).putInt("PM1EGen", 2).build());

        assertNull(LevelImportFormat.detect(directory, null));
    }

    @Test
    void detectsBackupMetadataWithoutRestoringTheSource(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder()
                .putString("generatorName", "normal")
                .putString("generatorOptions", "")
                .putInt("PM1EGen", 2)
                .build());
        Files.move(directory.resolve("level.dat"), directory.resolve("level.dat_old"));

        assertEquals(LevelImportFormat.LEVELDB, LevelImportFormat.detect(directory, null));
        assertFalse(Files.exists(directory.resolve("level.dat")));
    }

    @Test
    void generatorNamesCannotIdentifyTheStorageEncoding(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder()
                .putString("generatorName", "cloudburst:standard")
                .putString("generatorOptions", "")
                .build());

        assertThrows(IOException.class, () -> LevelImportFormat.detect(directory, null));
    }

    @Test
    void detectsAnvilRegions(@TempDir Path directory) throws Exception {
        Files.createDirectory(directory.resolve("region"));
        Files.createFile(directory.resolve("region/r.0.0.mca"));

        assertEquals(LevelImportFormat.ANVIL, LevelImportFormat.detect(directory, null));
    }

    @Test
    void interruptedPublicationCannotCreateEmptyWorld(@TempDir Path directory) throws Exception {
        Files.createDirectory(directory.resolve(".world.import-backup"));

        assertThrows(IOException.class, () -> LevelImportFormat.detect(directory.resolve("world"), null));
        assertFalse(Files.exists(directory.resolve("world")));
    }

    @Test
    void ambiguousLevelDBEncodingCanBeSelectedExplicitly(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder().putString("generatorName", "normal").build());

        assertEquals(LevelImportFormat.LEVELDB, LevelImportFormat.detect(directory, LevelDBImportProfile.NUKKIT));
    }

    @Test
    void mixedStorageDirectoriesAreRejected(@TempDir Path directory) throws Exception {
        writeMetadata(directory, NbtMap.builder().putInt("PM1EGen", 2).build());
        Files.createDirectory(directory.resolve("region"));
        Files.createFile(directory.resolve("region/r.0.0.mca"));

        assertThrows(IOException.class, () -> LevelImportFormat.detect(directory, null));
    }

    @Test
    void anUninitializedEmptyDatabaseDirectoryDoesNotRequireImport(@TempDir Path directory) throws Exception {
        Files.createDirectory(directory.resolve("db"));

        assertNull(LevelImportFormat.detect(directory, null));
        assertFalse(Files.exists(directory.resolve("level.dat")));
        assertFalse(Files.exists(directory.resolve("db/CURRENT")));
    }

    @Test
    void existingInvalidMetadataIsNotReplacedForAnEmptyDatabase(@TempDir Path directory) throws Exception {
        Files.createDirectory(directory.resolve("db"));
        Files.writeString(directory.resolve("level.dat"), "invalid");

        assertThrows(IOException.class, () -> LevelImportFormat.detect(directory, null));
        assertEquals("invalid", Files.readString(directory.resolve("level.dat")));
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
