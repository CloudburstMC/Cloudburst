package org.cloudburstmc.server.level.provider;

import org.cloudburstmc.server.level.LevelData;
import org.cloudburstmc.server.utils.LoadState;

import java.io.IOException;
import java.nio.file.Path;

public interface LevelDataSerializer {

    LoadState load(LevelData data, Path levelPath, String levelId) throws IOException;

    void save(LevelData data, Path levelPath, String levelId) throws IOException;
}
