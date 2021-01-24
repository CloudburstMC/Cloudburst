package org.cloudburstmc.server.world;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.world.provider.WorldProvider;
import org.cloudburstmc.server.world.provider.WorldProviderFactory;
import org.cloudburstmc.server.registry.StorageRegistry;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Log4j2
@NotThreadSafe
public class WorldBuilder {
    private final Server server;
    private final WorldData worldData;
    private String id;
    private Identifier storageId;
    //private PlayerDataProvider playerDataProvider;

    public WorldBuilder(Server server) {
        this.server = server;
        this.worldData = new WorldData(server.getDefaultWorldData());
    }

    public WorldBuilder id(String id) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id is null or empty");
        this.worldData.setName(this.id = id);
        return this;
    }

    public WorldBuilder seed(long seed) {
        this.worldData.setRandomSeed(seed);
        return this;
    }

    public WorldBuilder storage(Identifier storageId) {
        Preconditions.checkNotNull(storageId, "storageType");
        this.storageId = storageId;
        return this;
    }

    public WorldBuilder generator(Identifier generatorId) {
        Preconditions.checkNotNull(generatorId, "generatorId");
        Preconditions.checkArgument(this.server.getGeneratorRegistry().isRegistered(generatorId), "Unknown generator: \"%s\"", generatorId);
        this.worldData.setGenerator(generatorId);
        return this;
    }

    public WorldBuilder generatorOptions(String generatorOptions) {
        Preconditions.checkNotNull(generatorOptions, "generatorOptions");
        this.worldData.setGeneratorOptions(generatorOptions);
        return this;
    }

    @Nonnull
    public CompletableFuture<World> load() {
        World loadedWorld = this.server.getWorld(id);
        if (loadedWorld != null) {
            return CompletableFuture.completedFuture(loadedWorld);
        }
        final Path worldsPath = server.getDataPath().resolve("worlds");

        // If storageId isn't set detect or set default.
        if (storageId == null) {
            storageId = StorageRegistry.get().detectStorage(id, worldsPath);
            if (storageId == null) {
                storageId = server.getDefaultStorageId();
            }
        }

        final Executor executor = this.server.getWorldManager().getChunkExecutor();

        // Load chunk provider
        CompletableFuture<WorldProvider> providerFuture = CompletableFuture.supplyAsync(() -> {
            WorldProviderFactory factory = this.server.getStorageRegistry().getLevelProviderFactory(storageId);
            if (factory == null) {
                throw new IllegalArgumentException("Unregistered storageType");
            }
            try {
                WorldProvider provider = factory.create(id, worldsPath, executor);
                // Load world data
                provider.loadLevelData(worldData);
                return provider;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to load world provider for world '" + id + "'", e);
            }
        }, executor);

        // Combine futures
        return providerFuture.thenApply(levelProvider -> {
            World world = new World(this.server, id, levelProvider, worldData);
            this.server.getWorldManager().register(world);
            world.init();
            world.setTickRate(this.server.getBaseTickRate());
            return world;
        });
    }
}
