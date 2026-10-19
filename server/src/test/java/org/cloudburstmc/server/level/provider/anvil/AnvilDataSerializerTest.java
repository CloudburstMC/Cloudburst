package org.cloudburstmc.server.level.provider.anvil;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.persistence.NbtFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnvilDataSerializerTest {

    @Test
    void readsCompressedMetadataWithoutChangingTheSource(@TempDir Path directory) throws Exception {
        NbtMap root = NbtMap.builder().putCompound("Data", NbtMap.builder().putString("LevelName", "test").build()).build();
        Files.write(directory.resolve("level.dat"), NbtFiles.encodeCompressed(root));

        CloudLevelData loaded = AnvilDataSerializer.INSTANCE.load(new CloudLevelData(), directory);

        assertEquals("test", loaded.getName());
        assertEquals(root, NbtFiles.readCompressed(directory.resolve("level.dat")));
    }

    @Test
    void corruptPrimaryIsNotRestoredOrReplacedFromBackup(@TempDir Path directory) throws Exception {
        NbtMap root = NbtMap.builder().putCompound("Data", NbtMap.EMPTY).build();
        Files.write(directory.resolve("level.dat_old"), NbtFiles.encodeCompressed(root));
        Files.writeString(directory.resolve("level.dat"), "corrupt");

        assertThrows(IOException.class, () -> AnvilDataSerializer.INSTANCE.load(new CloudLevelData(), directory));

        assertEquals("corrupt", Files.readString(directory.resolve("level.dat")));
        assertEquals(root, NbtFiles.readCompressed(directory.resolve("level.dat_old")));

        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(2, files.count());
        }
    }

    @Test
    void missingMetadataFailsInsteadOfCreatingAnEmptyLevel(@TempDir Path directory) {
        assertThrows(IOException.class, () -> AnvilDataSerializer.INSTANCE.load(new CloudLevelData(), directory));
    }
}
