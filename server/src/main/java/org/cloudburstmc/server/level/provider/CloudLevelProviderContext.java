package org.cloudburstmc.server.level.provider;

import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.level.CloudLevelLoadRequest;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Dependencies and level state required to create a storage provider.
 *
 * @param server      owning server
 * @param request     immutable level request
 * @param initialData initialized state for a new level
 * @param levelsPath  root levels directory
 * @param executor    executor for storage work
 */
public record CloudLevelProviderContext(
        CloudServer server,
        CloudLevelLoadRequest request,
        CloudLevelData initialData,
        Path levelsPath,
        Executor executor
) {
    public CloudLevelProviderContext {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(initialData, "initialData");
        Objects.requireNonNull(levelsPath, "levelsPath");
        Objects.requireNonNull(executor, "executor");
    }
}
