package org.cloudburstmc.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerFileLayoutTest {

    @Test
    void levelPersistenceFollowsTheConfiguredLevelRoot(@TempDir Path directory) {
        Path levels = directory.resolve("custom-levels");
        ServerFileLayout files = new ServerFileLayout(directory, levels);

        assertEquals(levels.resolve("primary/playerdata"), files.playerData("primary"));
        assertEquals(levels.resolve("primary/data"), files.savedData("primary"));
        assertEquals(directory.resolve("usercache.json"), files.userCache());
        assertEquals(directory.resolve("logs/latest.log"), files.latestLog());
        assertEquals(directory.resolve("crash-reports"), files.crashReports());
        assertThrows(IllegalArgumentException.class, () -> files.playerData("../outside"));
    }
}
