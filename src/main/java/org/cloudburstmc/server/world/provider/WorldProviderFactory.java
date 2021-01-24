package org.cloudburstmc.server.world.provider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executor;

public interface WorldProviderFactory {

    /**
     * Creates new provider
     *
     * @param worldId    world ID
     * @param worldsPath path of the worlds directory (NOT THE WORLDS DIRECTORY ITSELF)
     * @param executor   executor to run tasks async
     * @return chunk provider
     * @throws IOException error created provider
     */
    WorldProvider create(String worldId, Path worldsPath, Executor executor) throws IOException;

    /**
     * Checks if world provider is compatible with directory given
     *
     * @param worldsPath path of the worlds directory (NOT THE WORLDS DIRECTORY ITSELF)
     * @param worldId    world ID
     * @return true if world is compatible.
     */
    boolean isCompatible(String worldId, Path worldsPath);
}
