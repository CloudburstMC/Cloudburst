package org.cloudburstmc.server.map;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.persistence.NbtFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MapDataFilesTest {

    @TempDir
    Path directory;

    @Test
    void currentFormatPreservesTheExplicitScaleHierarchy() throws Exception {
        Path file = directory.resolve("map_0.dat");
        NbtMap data = data();
        MapDataFiles.write(file, 0, data);

        NbtMap stored = MapDataFiles.read(file, 0);
        assertEquals(2, stored.getInt("version"));
        assertArrayEquals(new long[]{0, 1, 2, 3, 4}, stored.getLongArray("mapIds"));
        assertFalse(Files.exists(directory.resolve("map_0.tmp")));
    }

    @Test
    void oldFormatsAreRejectedRatherThanUpgraded() throws Exception {
        Path file = directory.resolve("map_0.dat");
        byte[] original = NbtFiles.encodeCompressed(data().toBuilder().putInt("version", 1).build());
        Files.write(file, original);

        assertThrows(IOException.class, () -> MapDataFiles.read(file, 0));
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    void missingOrDuplicateHierarchyIdsCannotReplaceAValidMap() throws Exception {
        Path file = directory.resolve("map_0.dat");
        MapDataFiles.write(file, 0, data());
        byte[] original = Files.readAllBytes(file);
        for (long[] ids : new long[][]{new long[0], {0, 1, 1, 3, 4}}) {
            NbtMap invalid = data().toBuilder().putLongArray("mapIds", ids).build();
            assertThrows(IOException.class, () -> MapDataFiles.write(file, 0, invalid));
            assertArrayEquals(original, Files.readAllBytes(file));
        }
    }

    private NbtMap data() {
        return NbtMap.builder().putInt("scale", 0).putIntArray("pixels", new int[128 * 128])
                .putLongArray("mapIds", new long[]{0, 1, 2, 3, 4}).build();
    }
}
