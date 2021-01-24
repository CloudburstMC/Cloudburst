package org.cloudburstmc.server.world.provider;

import org.cloudburstmc.server.world.WorldData;
import org.cloudburstmc.server.utils.LoadState;

import java.io.IOException;
import java.nio.file.Path;

public interface WorldDataSerializer {

    LoadState load(WorldData data, Path worldPath, String worldId) throws IOException;

    void save(WorldData data, Path worldPath, String worldId) throws IOException;
}
