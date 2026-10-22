package org.cloudburstmc.server.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudMapManagerTest {

    @Test
    void initializingMissingMapStorageDoesNotCreateTheLevelDirectory(@TempDir Path directory) {
        Path level = directory.resolve("world");
        CloudMapManager maps = new CloudMapManager(null, level.resolve("data"));

        assertTrue(maps.get(0).isEmpty());
        maps.save();

        assertFalse(Files.exists(level));
    }
}
