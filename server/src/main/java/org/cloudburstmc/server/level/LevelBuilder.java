package org.cloudburstmc.server.level;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Injector;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.inject.LevelModule;
import org.cloudburstmc.server.level.provider.LevelProvider;
import org.cloudburstmc.server.level.provider.LevelProviderFactory;
import org.cloudburstmc.server.registry.StorageRegistry;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Log4j2
@NotThreadSafe
public class LevelBuilder {
    private final CloudServer server;
    private final LevelData levelData;
    private String id;
    private Identifier storageId;
    //private PlayerDataProvider playerDataProvider;

    public LevelBuilder(CloudServer server) {
        this.server = server;
        this.levelData = new LevelData(server.getDefaultLevelData());
    }

    public LevelBuilder id(String id) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id is null or empty");
        this.levelData.setName(this.id = id);
        return this;
    }

    public LevelBuilder seed(long seed) {
        this.levelData.setRandomSeed(seed);
        return this;
    }

    public LevelBuilder storage(Identifier storageId) {
        Preconditions.checkNotNull(storageId, "storageType");
        this.storageId = storageId;
        return this;
    }

    public LevelBuilder generator(Identifier generatorId) {
        Preconditions.checkNotNull(generatorId, "generatorId");
        Preconditions.checkArgument(this.server.getGeneratorRegistry().isRegistered(generatorId), "Unknown generator: \"%s\"", generatorId);
        this.levelData.setGenerator(generatorId);
        return this;
    }

    public LevelBuilder generatorOptions(String generatorOptions) {
        // Preconditions.checkNotNull(generatorOptions, "generatorOptions"); This should be able to be null, since we have defaults on the generator classes
        this.levelData.setGeneratorOptions(generatorOptions);
        return this;
    }

    @NonNull
    public CompletableFuture<CloudLevel> load() {
        CloudLevel loadedLevel = this.server.getLevel(id);
        if (loadedLevel != null) {
            return CompletableFuture.completedFuture(loadedLevel);
        }

        final Path worldsPath = server.getDataPath().resolve("worlds");

        // If storageId isn't set, detect or fall back to the server default.
        if (storageId == null) {
            storageId = StorageRegistry.get().detectStorage(id, worldsPath);
            if (storageId == null) {
                storageId = server.getDefaultStorageId();
            }
        }

        final Executor executor = this.server.getLevelManager().getIoExecutor();

        log.info(server.getLanguage().translate("cloudburst.level.preparing", "§a" + id + "§r"));

        final long startTime = System.currentTimeMillis();

        return loadProvider(executor, worldsPath)
                .thenApply(this::createLevel)
                .thenApply(level -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.info("Prepared level \"§a{}§r\" in §a{}s§r", id, String.format("%.2f", elapsed / 1000d));
                    return level;
                });
    }

    private CompletableFuture<LevelProvider> loadProvider(Executor executor, Path worldsPath) {
        return CompletableFuture.supplyAsync(() -> {
            LevelProviderFactory factory = this.server.getStorageRegistry().getLevelProviderFactory(storageId);
            if (factory == null) {
                throw new IllegalArgumentException("Unregistered storageType");
            }
            try {
                LevelProvider provider = factory.create(id, worldsPath, executor);
                provider.loadLevelData(levelData);
                return provider;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to load level provider for level '" + id + "'", e);
            }
        }, executor);
    }

    private CloudLevel createLevel(LevelProvider levelProvider) {
        Injector injector = server.getInjector().createChildInjector(new LevelModule(id, levelProvider, levelData));
        CloudLevel level = injector.getInstance(CloudLevel.class);
        this.server.getLevelManager().register(level);
        level.init();
        level.setTickRate(this.server.getBaseTickRate());
        return level;
    }

}
