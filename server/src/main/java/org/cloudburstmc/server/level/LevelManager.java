package org.cloudburstmc.server.level;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.level.LevelLoadEvent;
import org.cloudburstmc.api.event.level.LevelUnloadEvent;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.configuration.GlobalConfiguration;
import org.cloudburstmc.server.diagnostics.ServerExceptionHandler;
import org.cloudburstmc.server.inject.LevelModule;
import org.cloudburstmc.server.inject.qualifier.LevelDirectory;
import org.cloudburstmc.server.level.provider.CloudLevelProviderContext;
import org.cloudburstmc.server.level.provider.LevelImportFormat;
import org.cloudburstmc.server.level.provider.LevelProvider;
import org.cloudburstmc.server.level.provider.LevelProviderFactory;
import org.cloudburstmc.server.level.provider.migration.LevelDBImportProfile;
import org.cloudburstmc.server.level.provider.migration.LevelDBImportSource;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.StorageRegistry;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates level loading, registration, ticking, saving and shutdown.
 * Owns the shared loading, generation and I/O executors until it is closed.
 */
@Log4j2
@Singleton
public class LevelManager implements Closeable {

    private final CloudServer server;
    private final ExecutorService loadExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Cloudburst Level Load #", 0).factory());

    /**
     * Parent directory containing the configured worlds.
     */
    @Getter
    private final Path levelsPath;

    private final Set<CloudLevel> levels = new HashSet<>();
    private final Map<String, CloudLevel> levelIds = new HashMap<>();
    private final Map<String, CompletableFuture<CloudLevel>> loadingLevels = new HashMap<>();

    private volatile ExecutorService ioExecutor;
    private volatile ForkJoinPool generationExecutor;

    /**
     * Default destination for players leaving an unloaded level, or {@code null} until selected.
     */
    @Getter
    private volatile CloudLevel defaultLevel;

    private boolean closed;

    /**
     * Creates a manager without loading levels or initializing the shared chunk executors.
     *
     * @param server     server owning the levels
     * @param levelsPath parent directory for world storage
     */
    @Inject
    public LevelManager(CloudServer server, @LevelDirectory Path levelsPath) {
        this.server = server;
        this.levelsPath = levelsPath;
    }

    /**
     * Creates shared chunk executors before any level can be loaded.
     *
     * @param settings thread counts for generation and storage work
     * @throws IllegalStateException if the manager is closed or executors already exist
     */
    public synchronized void initializeExecutors(GlobalConfiguration.ChunkSystem settings) {
        Preconditions.checkState(!this.closed, "Level manager is closed");
        Preconditions.checkState(this.ioExecutor == null, "Chunk executors are already initialized");

        int ioThreads = settings.resolveIoThreads();
        int workerThreads = settings.resolveWorkerThreads(Runtime.getRuntime().availableProcessors());

        this.generationExecutor = new ForkJoinPool(workerThreads, new GenerationThreadFactory(), ServerExceptionHandler::dispatch, false);
        this.ioExecutor = Executors.newFixedThreadPool(ioThreads, Thread.ofPlatform()
                .name("Cloudburst Chunk I/O #", 0)
                .daemon(true)
                .uncaughtExceptionHandler(ServerExceptionHandler::dispatch)
                .factory());

        log.info("Chunk system is using {} worker threads and {} I/O threads", workerThreads, ioThreads);
    }

    /**
     * Returns the manager-owned I/O executor after {@link #initializeExecutors} has been called.
     */
    public ExecutorService getIoExecutor() {
        return Objects.requireNonNull(this.ioExecutor, "Chunk executors have not been initialized");
    }

    /**
     * Returns the manager-owned generation pool after {@link #initializeExecutors} has been called.
     */
    public ForkJoinPool getGenerationExecutor() {
        return Objects.requireNonNull(this.generationExecutor, "Chunk executors have not been initialized");
    }

    /**
     * Imports a combined default Bedrock world before any configured level is opened.
     * Other source formats are left for individual level loading.
     *
     * @param requests  enabled startup level definitions
     * @param defaultId ID of the enabled default level
     * @throws IllegalStateException if this manager is closed or levels have started loading
     * @throws IOException           if the default level is absent, dimension destinations are ambiguous
     *                               or the import cannot be completed
     */
    public synchronized void prepareConfiguredLevels(List<CloudLevelLoadRequest> requests, String defaultId) throws IOException {
        Preconditions.checkState(!this.closed, "Level manager is closed");
        Preconditions.checkState(this.levels.isEmpty() && this.loadingLevels.isEmpty(), "Levels have already started loading");
        CloudLevelLoadRequest primary = requests.stream()
                .filter(request -> request.id().equals(defaultId))
                .findFirst()
                .orElseThrow(() -> new IOException("Default level is not enabled or configured: " + defaultId));
        Path source = this.levelsPath.resolve(primary.id());
        if (primary.dimension() != CloudLevel.DIMENSION_OVERWORLD
                || LevelImportFormat.detect(source, primary.importProfile()) != LevelImportFormat.LEVELDB
                || LevelDBImportProfile.detect(LevelDBImportSource.readMetadata(source), primary.importProfile()) != LevelDBImportProfile.BEDROCK) {
            return;
        }

        List<CloudLevelLoadRequest> dimensions = new ArrayList<>();
        dimensions.add(primary);
        for (int dimension = CloudLevel.DIMENSION_NETHER; dimension <= CloudLevel.DIMENSION_THE_END; dimension++) {
            int selected = dimension;
            List<CloudLevelLoadRequest> targets = requests.stream()
                    .filter(request -> request.dimension() == selected)
                    .toList();
            if (targets.size() > 1) {
                throw new IOException("Combined Bedrock import requires one configured destination for dimension " + dimension);
            }

            dimensions.addAll(targets);
        }

        CloudLevelImporter.importDimensions(this.server, this.levelsPath, dimensions, source, this.getIoExecutor());
    }

    /**
     * Loads, imports or creates a level. Concurrent requests for the same ID share one operation.
     * An already loaded level is returned without applying the new request's settings.
     *
     * @param request immutable load configuration
     * @return future completed with the initialized and registered level
     * @throws IllegalStateException if the manager is closed or executors are not initialized
     */
    public synchronized CompletableFuture<CloudLevel> load(CloudLevelLoadRequest request) {
        Objects.requireNonNull(request, "request");
        Preconditions.checkState(!this.closed, "Level manager is closed");
        Preconditions.checkState(this.ioExecutor != null, "Chunk executors have not been initialized");

        CloudLevel loadedLevel = this.levelIds.get(request.id());
        if (loadedLevel != null) {
            return CompletableFuture.completedFuture(loadedLevel);
        }

        CompletableFuture<CloudLevel> currentLoad = this.loadingLevels.get(request.id());
        if (currentLoad != null) {
            return currentLoad;
        }

        CompletableFuture<CloudLevel> load = CompletableFuture.supplyAsync(() -> this.loadNewLevel(request), this.loadExecutor);
        this.loadingLevels.put(request.id(), load);
        load.whenComplete((level, throwable) -> {
            synchronized (this) {
                this.loadingLevels.remove(request.id(), load);
            }
        });

        return load;
    }

    /**
     * Registers an initialized level owned by this server.
     * Fires {@link LevelLoadEvent} before the level becomes available through lookups.
     *
     * @param level level to register
     * @throws IllegalStateException if this manager is closed
     * @throws IllegalArgumentException if the level belongs to another server or its ID is registered
     */
    public synchronized void register(CloudLevel level) {
        Preconditions.checkNotNull(level, "level");
        Preconditions.checkState(!this.closed, "Level manager is closed");
        Preconditions.checkArgument(level.getServer() == this.server, "Level did not come from this server");
        Preconditions.checkArgument(!levels.contains(level), "level already registered");
        Preconditions.checkArgument(!levelIds.containsKey(level.getId()), "level ID already registered: %s", level.getId());

        LevelLoadEvent event = new LevelLoadEvent(level);
        this.server.getEventManager().fire(event);

        this.levels.add(level);
        this.levelIds.put(level.getId(), level);
    }

    /**
     * Unloads and deregisters a non-default level.
     * Players move to the default level or are disconnected if no default exists.
     *
     * @param level level owned by this manager
     * @return {@code true} if unloaded, or {@code false} for the default level or a canceled event
     * @throws IllegalArgumentException if the level is not registered
     */
    public boolean unload(CloudLevel level) {
        Preconditions.checkNotNull(level, "level");
        synchronized (this) {
            Preconditions.checkArgument(this.levels.contains(level), "level not registered");
            if (level == this.defaultLevel) {
                return false;
            }
        }

        LevelUnloadEvent event = new LevelUnloadEvent(level);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        CloudLevel fallback;
        synchronized (this) {
            if (level == this.defaultLevel) {
                return false;
            }
            Preconditions.checkState(this.levels.remove(level), "level was unloaded concurrently");
            this.levelIds.remove(level.getId());
            fallback = this.defaultLevel;
        }

        log.info("Unloading world '{}'", level.getId());
        for (CloudPlayer player : List.copyOf(level.getPlayers().values())) {
            if (fallback == null) {
                player.close(player.leaveMessage(), "Forced default level unload");
            } else {
                player.teleport(fallback.getSafeSpawn());
            }
        }

        level.close();
        return true;
    }

    /**
     * Returns the loaded level with this storage ID, or {@code null} if it is not loaded.
     */
    @Nullable
    public synchronized CloudLevel getLevel(String id) {
        return this.levelIds.get(id);
    }

    /**
     * Returns a loaded level with the given saved name, or {@code null} if none matches.
     * Matching is case-sensitive. Use {@link #getLevel(String)} for storage IDs.
     */
    @Nullable
    public synchronized CloudLevel getLevelByName(String name) {
        for (CloudLevel level : this.levels) {
            if (level.getName().equals(name)) {
                return level;
            }
        }

        return null;
    }

    /**
     * Returns an immutable snapshot of the registered levels.
     */
    public synchronized Set<CloudLevel> getLevels() {
        return ImmutableSet.copyOf(levels);
    }

    /**
     * Selects the default level without loading or registering it.
     *
     * @param level registered level to use as the default
     * @throws IllegalArgumentException if the level is not registered
     */
    public synchronized void setDefaultLevel(CloudLevel level) {
        Preconditions.checkNotNull(level, "level");
        Preconditions.checkArgument(levels.contains(level), "level not registered");

        this.defaultLevel = level;
    }

    /**
     * Requests saves for registered levels whose autosave is enabled.
     * Does not wait for storage writes to complete.
     */
    public synchronized void save() {
        this.levels.forEach(CloudLevel::save);
    }

    /**
     * Advances registered levels and performs periodic chunk cleanup.
     *
     * @param currentTick current server tick
     */
    public void tick(int currentTick) {
        for (CloudLevel level : this.levels) {
            try {
                long levelTime = System.nanoTime();
                level.doTick(currentTick);
                level.setLastTickDuration((System.nanoTime() - levelTime) / 1_000_000d);

                if (currentTick % 100 == 0) {
                    level.doChunkGarbageCollection();
                }
            } catch (Exception e) {
                log.error("Could not tick world '{}'", level.getId(), e);
            }
        }
    }

    /**
     * Rejects further loads, waits for pending loads and closes registered levels.
     * Shuts down the owned executors after closing the levels. Repeated calls have no effect.
     */
    @Override
    public void close() {
        List<CompletableFuture<CloudLevel>> pendingLoads;
        synchronized (this) {
            if (this.closed) {
                return;
            }
            this.closed = true;
            pendingLoads = List.copyOf(this.loadingLevels.values());
        }

        CompletableFuture.allOf(pendingLoads.toArray(CompletableFuture<?>[]::new))
                .exceptionally(throwable -> null)
                .join();

        synchronized (this) {
            for (CloudLevel level : this.levels) {
                try {
                    level.close();
                } catch (Exception e) {
                    log.error("Error closing world '{}'", level.getId(), e);
                }
            }
        }

        shutdownExecutor(this.loadExecutor, "Level load");
        shutdownExecutor(this.generationExecutor, "Generation");
        shutdownExecutor(this.ioExecutor, "I/O");
    }

    private CloudLevel loadNewLevel(CloudLevelLoadRequest request) {
        long startTime = System.nanoTime();
        try {
            Path source = this.levelsPath.resolve(request.id());
            LevelImportFormat format = LevelImportFormat.detect(source, request.importProfile());
            if (format != null) {
                CloudLevelImporter.importLevel(this.server, this.levelsPath, request, format, source, this.ioExecutor);
            }
        } catch (IOException failure) {
            throw new CompletionException("Unable to prepare world '" + request.id() + "'", failure);
        }

        this.server.getConfig().createLevelConfiguration(request.id());
        CloudLevelData initialData = request.createInitialData(this.server.getLevelDefaults());
        StorageRegistry storageRegistry = this.server.getStorageRegistry();
        Identifier storage = request.storage();
        if (storage == null) {
            storage = storageRegistry.detectStorage(request.id(), this.levelsPath);
        }

        if (storage == null) {
            storage = this.server.getDefaultStorageId();
        }

        Identifier selectedStorage = storage;
        LevelProviderFactory factory = Objects.requireNonNull(
                storageRegistry.getLevelProviderFactory(selectedStorage),
                () -> "Unknown storage provider: " + selectedStorage
        );

        CloudLevelProviderContext context = new CloudLevelProviderContext(
                this.server,
                request,
                initialData,
                this.levelsPath,
                this.ioExecutor
        );

        LevelProvider provider = null;
        CloudLevel level;
        try {
            provider = factory.create(context);
            CloudLevelData data = provider.loadLevelData(initialData).join().orElse(initialData);
            data.setDimension(request.dimension());

            level = this.server.getInjector()
                    .createChildInjector(new LevelModule(request.id(), provider, data))
                    .getInstance(CloudLevel.class);
            level.init();
            this.register(level);
        } catch (Throwable throwable) {
            closeAfterFailedLoad(provider, throwable);
            throw asCompletionException(throwable);
        }

        log.info("Loaded world '{}' in {} ms", request.id(), (System.nanoTime() - startTime) / 1_000_000L);
        return level;
    }

    private static void closeAfterFailedLoad(@Nullable LevelProvider provider, Throwable failure) {
        if (provider == null) {
            return;
        }

        try {
            provider.close();
        } catch (Exception closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static CompletionException asCompletionException(Throwable throwable) {
        if (throwable instanceof CompletionException completionException) {
            return completionException;
        }

        return new CompletionException(throwable);
    }

    private static void shutdownExecutor(@Nullable ExecutorService executor, String name) {
        if (executor == null) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("{} executor did not terminate in time, forcing shutdown", name);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Names worker threads in the generation pool for easy identification
     * in thread dumps and profiler output.
     */
    private static final class GenerationThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setName("Cloudburst Chunk Generation #" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
