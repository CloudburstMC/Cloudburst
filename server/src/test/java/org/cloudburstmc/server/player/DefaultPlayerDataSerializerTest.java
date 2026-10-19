package org.cloudburstmc.server.player;

import org.cloudburstmc.api.util.PlayerDataKey;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.persistence.NbtFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPlayerDataSerializerTest {

    @TempDir
    Path directory;

    @Test
    void onlyMissingDataReturnsAnEmptyResult() throws Exception {
        PlayerDataKey key = PlayerDataKey.of(UUID.randomUUID());
        DefaultPlayerDataSerializer serializer = new DefaultPlayerDataSerializer(directory);
        assertTrue(serializer.read(key).isEmpty());

        Path file = path(key);
        Files.writeString(file, "corrupt");

        assertThrows(IOException.class, () -> serializer.read(key));
        assertEquals("corrupt", Files.readString(file));
    }

    @Test
    void corruptPrimaryRecoversTheBackupAndPreservesTheOriginal() throws Exception {
        PlayerDataKey key = PlayerDataKey.of(UUID.randomUUID());
        DefaultPlayerDataSerializer serializer = new DefaultPlayerDataSerializer(directory);
        NbtMap first = NbtMap.builder().putInt("value", 1).build();
        serializer.write(key, first);
        serializer.write(key, NbtMap.builder().putInt("value", 2).build());
        Files.writeString(path(key), "corrupt");

        assertEquals(first, serializer.read(key).orElseThrow());
        assertEquals(first, NbtFiles.readCompressed(path(key)));
        try (Stream<Path> files = Files.list(directory)) {
            Path preserved = files.filter(file -> file.getFileName().toString().contains(".corrupt-"))
                    .findFirst().orElseThrow();
            assertEquals("corrupt", Files.readString(preserved));
        }

        serializer.write(key, NbtMap.builder().putInt("value", 3).build());
        assertEquals(first, NbtFiles.readCompressed(backup(key)));
    }

    @Test
    void missingPrimaryRecoversAnExistingBackup() throws Exception {
        PlayerDataKey key = PlayerDataKey.of(UUID.randomUUID());
        DefaultPlayerDataSerializer serializer = new DefaultPlayerDataSerializer(directory);
        NbtMap data = NbtMap.builder().putInt("value", 1).build();
        serializer.write(key, data);
        Files.move(path(key), backup(key));

        assertEquals(data, serializer.read(key).orElseThrow());
        assertEquals(data, NbtFiles.readCompressed(path(key)));
    }

    @Test
    void unusablePrimaryAndBackupAreNotReplacedWithFreshData() throws Exception {
        PlayerDataKey key = PlayerDataKey.of(UUID.randomUUID());
        DefaultPlayerDataSerializer serializer = new DefaultPlayerDataSerializer(directory);
        Files.writeString(path(key), "bad primary");
        Files.writeString(backup(key), "bad backup");

        assertThrows(IOException.class, () -> serializer.read(key));
        assertEquals("bad primary", Files.readString(path(key)));
        assertEquals("bad backup", Files.readString(backup(key)));
    }

    @Test
    void corruptPrimaryDoesNotOverwriteTheGoodBackupDuringASave() throws Exception {
        PlayerDataKey key = PlayerDataKey.of(UUID.randomUUID());
        DefaultPlayerDataSerializer serializer = new DefaultPlayerDataSerializer(directory);
        NbtMap first = NbtMap.builder().putInt("value", 1).build();
        serializer.write(key, first);
        serializer.write(key, NbtMap.builder().putInt("value", 2).build());
        Files.writeString(path(key), "corrupt");
        serializer.write(key, NbtMap.builder().putInt("value", 3).build());

        assertEquals(first, NbtFiles.readCompressed(backup(key)));
        assertEquals(3, serializer.read(key).orElseThrow().getInt("value"));
    }

    private Path path(PlayerDataKey key) {
        return directory.resolve(key.uniqueId() + ".dat");
    }

    private Path backup(PlayerDataKey key) {
        return directory.resolve(key.uniqueId() + ".dat_old");
    }
}
