package org.cloudburstmc.server.level.manager;

import co.aikar.timings.Timing;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.spotify.futures.CompletableFutures;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.level.ChunkUnloadEvent;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.server.config.ServerConfig;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.LevelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

@Log4j2
public final class LevelChunkManager {
    private static final CompletableFuture<Void> COMPLETED_VOID_FUTURE = CompletableFuture.completedFuture(null);

    /**
     * How often dirty pending-tick containers are flushed to disk
     * for chunks that remain loaded. This is independent of the full
     * chunk autosave interval and ensures a server crash does not discard
     * more than this many ticks worth of scheduled block updates.
     */
    private static final int PENDING_TICK_SAVE_INTERVAL = 300;
    private static final AtomicIntegerFieldUpdater<LoadingChunk> GENERATION_RUNNING_UPDATER = AtomicIntegerFieldUpdater.newUpdater(LoadingChunk.class, "generationRunning");
    private static final AtomicIntegerFieldUpdater<LoadingChunk> POPULATION_RUNNING_UPDATER = AtomicIntegerFieldUpdater.newUpdater(LoadingChunk.class, "populationRunning");
    private static final AtomicIntegerFieldUpdater<LoadingChunk> FINISH_RUNNING_UPDATER = AtomicIntegerFieldUpdater.newUpdater(LoadingChunk.class, "finishRunning");
    private static final AtomicIntegerFieldUpdater<LoadingChunk> CLOSED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(LoadingChunk.class, "closed");

    private final CloudLevel level;
    private final LevelProvider provider;
    private final ConcurrentHashMap<Long, LoadingChunk> chunks = new ConcurrentHashMap<>();
    private final Executor executor;

    /**
     * Counts server ticks to gate the periodic pending-tick flush.
     */
    private int pendingTickSaveTicker = 0;

    public LevelChunkManager(CloudLevel level) {
        this(level, level.getProvider());
    }

    public LevelChunkManager(CloudLevel level, LevelProvider provider) {
        this.level = level;
        this.executor = level.getServer().getLevelManager().getGenerationExecutor();
        this.provider = provider;
    }

    /**
     * Returns a set of all loaded chunks in this level.
     *
     * @return chunks
     */
    @NonNull
    public Set<CloudChunk> getLoadedChunks() {
        ImmutableSet.Builder<CloudChunk> chunks = ImmutableSet.builder();
        for (LoadingChunk loadingChunk : this.chunks.values()) {
            CloudChunk chunk = loadingChunk.getChunk();
            if (chunk != null) {
                chunks.add(chunk);
            }
        }
        return chunks.build();
    }

    public int getLoadedCount() {
        return this.chunks.size();
    }

    /**
     * Get chunk at specified coordinate if it is already loaded.
     *
     * @param key chunk key
     * @return chunk or null
     */
    @Nullable
    public CloudChunk getLoadedChunk(long key) {
        LoadingChunk chunk = this.chunks.get(key);
        return chunk == null ? null : chunk.getChunk();
    }

    /**
     * Get chunk at specified coordinate if it is already loaded.
     *
     * @param x chunk x
     * @param z chunk z
     * @return chunk or null
     */
    @Nullable
    public CloudChunk getLoadedChunk(int x, int z) {
        return getLoadedChunk(CloudChunk.key(x, z));
    }

    /**
     * Get chunk at specified coordinate. This will block the current thread until the chunk is loaded.
     * <p>
     * Must not be called from a generation pool thread. Doing so would block
     * a CPU-bound worker while waiting for a future that may itself require
     * a free worker to complete, causing a deadlock.
     *
     * @param x chunk x
     * @param z chunk z
     * @return chunk
     */
    @NonNull
    public CloudChunk getChunk(int x, int z) {
        if (Thread.currentThread() instanceof ForkJoinWorkerThread) {
            throw new IllegalStateException("getChunk() must not be called from a generation pool thread");
        }

        CloudChunk chunk = getLoadedChunk(x, z);
        if (chunk == null) {
            chunk = this.getChunkFuture(x, z).join();
        }

        return chunk;
    }

    /**
     * Get chunk future at specified coordinate.
     *
     * @param x chunk x
     * @param z chunk z
     * @return chunk future
     */
    @NonNull
    public CompletableFuture<CloudChunk> getChunkFuture(int x, int z) {
        return this.getChunkFuture(x, z, true, true, true);
    }

    @NonNull
    private CompletableFuture<CloudChunk> getChunkFuture(int chunkX, int chunkZ, boolean generate, boolean populate, boolean finish) {
        final long chunkKey = CloudChunk.key(chunkX, chunkZ);
        LoadingChunk chunk = this.chunks.computeIfAbsent(chunkKey, key -> new LoadingChunk(key, true));
        chunk.lastAccessTime = System.currentTimeMillis();

        synchronized (chunk) {
            if (finish) {
                chunk.finish();
            } else if (populate) {
                chunk.populate();
            } else if (generate) {
                chunk.generate();
            }
            return chunk.getFuture();
        }
    }

    public boolean isChunkLoaded(long hash) {
        LoadingChunk chunk = this.chunks.get(hash);
        return chunk != null && chunk.getChunk() != null;
    }

    public boolean isChunkLoaded(int x, int z) {
        return this.isChunkLoaded(CloudChunk.key(x, z));
    }

    public boolean unloadChunk(long hash) {
        return this.unloadChunk(hash, true, true);
    }

    public boolean unloadChunk(CloudChunk chunk) {
        return this.unloadChunk(chunk, true);
    }

    public boolean unloadChunk(CloudChunk chunk, boolean save) {
        return this.unloadChunk(chunk, save, true);
    }

    public boolean unloadChunk(CloudChunk chunk, boolean save, boolean safe) {
        Preconditions.checkNotNull(chunk, "chunk");
        Preconditions.checkArgument(chunk.getLevel() == this.level, "Chunk is not from this level");
        return this.unloadChunk(CloudChunk.key(chunk.getX(), chunk.getZ()), save, safe);
    }

    public boolean unloadChunk(long chunkKey, boolean save, boolean safe) {
        LoadingChunk loadingChunk = this.chunks.get(chunkKey);
        if (loadingChunk == null) {
            return false;
        }
        if (tryUnload(chunkKey, loadingChunk, save, safe)) {
            this.chunks.remove(chunkKey, loadingChunk);
            return true;
        }
        return false;
    }

    private boolean tryUnload(long chunkKey, LoadingChunk loadingChunk, boolean save, boolean safe) {
        CloudChunk chunk = loadingChunk.getChunk();
        if (chunk == null) {
            return false;
        }
        if (chunk.hasLoaders()) {
            return false;
        }

        try (Timing ignored = this.level.timings.doChunkUnload.startTiming()) {
            ChunkUnloadEvent chunkUnloadEvent = new ChunkUnloadEvent(chunk);
            this.level.getServer().getEventManager().fire(chunkUnloadEvent);
            if (chunkUnloadEvent.isCancelled()) {
                return false;
            }

            if (safe && !this.level.getChunkPlayers(chunk.getX(), chunk.getZ()).isEmpty()) {
                return false;
            }

            if (!CLOSED_UPDATER.compareAndSet(loadingChunk, 0, 1)) {
                return false;
            }

            LevelChunkManager.this.level.getUpdateQueue().unregisterTickContainer(chunkKey);
            CompletableFuture<?> saveFuture = save ? this.saveChunk(chunk) : COMPLETED_VOID_FUTURE;

            saveFuture.whenComplete((r, ex) -> {
                LevelChunkManager.this.level.getUpdateQueue().removeTickContainer(chunkKey);
                LevelChunkManager.this.level.getServer().getGlobalScheduler().execute(null, chunk::close);
            });
            return true;
        }
    }

    public CompletableFuture<Void> saveChunks() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (LoadingChunk loadingChunk : this.chunks.values()) {
            CloudChunk chunk = loadingChunk.getChunk();
            if (chunk != null) {
                futures.add(saveChunk(chunk));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Void> saveChunk(Chunk chunk) {
        Preconditions.checkNotNull(chunk, "chunk");
        Preconditions.checkArgument(chunk.getLevel() == this.level, "Chunk is not from this ChunkManager's Level");
        if (chunk.isDirty()) {
            return this.provider.saveChunk(chunk).exceptionally(throwable -> {
                log.warn("Unable to save chunk", throwable);
                return null;
            });
        }
        return COMPLETED_VOID_FUTURE;
    }

    public void tick() {
        if (this.chunks.isEmpty()) {
            return;
        }

        long time = System.currentTimeMillis();

        final int spawnX = this.level.getSafeSpawn().getChunkX();
        final int spawnZ = this.level.getSafeSpawn().getChunkZ();
        final int spawnRadius = this.level.getSpawnChunkRadius();

        ServerConfig serverConfig = this.level.getServer().getConfig();

        boolean doTickSave = (++pendingTickSaveTicker >= PENDING_TICK_SAVE_INTERVAL);
        if (doTickSave) {
            pendingTickSaveTicker = 0;
        }

        try (Timing ignored = this.level.timings.doChunkGC.startTiming()) {
            for (var iter = this.chunks.entrySet().iterator(); iter.hasNext(); ) {
                var entry = iter.next();
                long chunkKey = entry.getKey();
                LoadingChunk loadingChunk = entry.getValue();
                CloudChunk chunk = loadingChunk.getChunk();
                if (chunk == null) {
                    continue;
                }

                if (doTickSave && this.level.getUpdateQueue().isDirty(chunkKey, this.level.getCurrentTick())) {
                    this.provider.savePendingTicks(chunk).exceptionally(throwable -> {
                        log.warn("Failed to incrementally save pending ticks for chunk ({}, {})", chunk.getX(), chunk.getZ(), throwable);
                        return null;
                    });
                }

                if ((Math.abs(chunk.getX() - spawnX) <= spawnRadius && Math.abs(chunk.getZ() - spawnZ) <= spawnRadius) ||
                        chunk.hasLoaders()) {
                    continue;
                }

                long loadedTime = loadingChunk.loadedTime;
                if (loadedTime == 0 || (time - loadedTime) <= TimeUnit.SECONDS.toMillis(serverConfig.getLevelSettings().getChunkTimeoutAfterLoad())) {
                    continue;
                }

                long lastAccessTime = loadingChunk.lastAccessTime;
                if (lastAccessTime == 0 || (time - lastAccessTime) <= TimeUnit.SECONDS.toMillis(serverConfig.getLevelSettings().getChunkTimeoutAfterLastAccess())) {
                    continue;
                }

                if (tryUnload(chunkKey, loadingChunk, true, true)) {
                    iter.remove();
                }
            }
        }
    }

    @ToString
    private class LoadingChunk {

        private final int x;
        private final int z;
        volatile int generationRunning;
        volatile int populationRunning;
        volatile int finishRunning;
        volatile int closed;
        volatile long loadedTime;
        volatile long lastAccessTime;
        private CompletableFuture<CloudChunk> future;
        private volatile CloudChunk chunk;

        public LoadingChunk(long key, boolean load) {
            this.x = CloudChunk.fromKeyX(key);
            this.z = CloudChunk.fromKeyZ(key);

            if (load) {
                this.future = LevelChunkManager.this.provider.readChunk(new ChunkBuilder(x, z, LevelChunkManager.this.level))
                        .thenApply(chunk -> {
                            if (chunk == null) {
                                return new CloudChunk(this.x, this.z, LevelChunkManager.this.level);
                            }
                            chunk.init();
                            return chunk;
                        });
                this.future.whenComplete((chunk, throwable) -> {
                    if (throwable != null) {
                        log.warn(new ParameterizedMessage("Unable to load chunk ({}, {}) in level {} ",
                                this.x, this.z, LevelChunkManager.this.level.getId()), throwable);
                        LevelChunkManager.this.chunks.remove(key, this);
                    } else {
                        this.loadedTime = System.currentTimeMillis();
                    }
                });
            } else {
                this.future = CompletableFuture.completedFuture(new CloudChunk(x, z, LevelChunkManager.this.level));
            }
            this.future.whenComplete((chunk, throwable) -> this.chunk = chunk);
        }

        public CompletableFuture<CloudChunk> getFuture() {
            return this.future;
        }

        @Nullable
        private CloudChunk getChunk() {
            CloudChunk c = this.chunk;
            if (c != null && c.isGenerated() && c.isPopulated() && c.isFinished()) {
                return c;
            }
            return null;
        }

        private void generate() {
            if ((this.chunk == null || !this.chunk.isGenerated()) && GENERATION_RUNNING_UPDATER.compareAndSet(this, 0, 1)) {
                this.future = this.future.thenApplyAsync(GenerationTask.INSTANCE, LevelChunkManager.this.executor);
                this.future.whenComplete((r, ex) -> GENERATION_RUNNING_UPDATER.compareAndSet(this, 1, 0));
            }
        }

        private void populate() {
            this.generate();
            if ((this.chunk == null || !this.chunk.isPopulated()) && POPULATION_RUNNING_UPDATER.compareAndSet(this, 0, 1)) {
                List<CompletableFuture<CloudChunk>> chunksToLoad = new ArrayList<>(8);
                for (int z = this.z - 1, maxZ = this.z + 1; z <= maxZ; z++) {
                    for (int x = this.x - 1, maxX = this.x + 1; x <= maxX; x++) {
                        if (x == this.x && z == this.z) continue;
                        chunksToLoad.add(LevelChunkManager.this.getChunkFuture(x, z, true, false, false));
                    }
                }
                CompletableFuture<List<CloudChunk>> aroundFuture = CompletableFutures.allAsList(chunksToLoad);
                this.future = this.future.thenCombineAsync(aroundFuture, PopulationTask.INSTANCE, LevelChunkManager.this.executor);
                this.future.whenComplete((r, ex) -> POPULATION_RUNNING_UPDATER.compareAndSet(this, 1, 0));
            }
        }

        private void finish() {
            this.populate();
            if ((this.chunk == null || !this.chunk.isFinished()) && FINISH_RUNNING_UPDATER.compareAndSet(this, 0, 1)) {
                List<CompletableFuture<CloudChunk>> chunksToLoad = new ArrayList<>(8);
                for (int z = this.z - 1, maxZ = this.z + 1; z <= maxZ; z++) {
                    for (int x = this.x - 1, maxX = this.x + 1; x <= maxX; x++) {
                        if (x == this.x && z == this.z) continue;
                        chunksToLoad.add(LevelChunkManager.this.getChunkFuture(x, z, true, true, false));
                    }
                }

                CompletableFuture<List<CloudChunk>> aroundFuture = CompletableFutures.allAsList(chunksToLoad);
                this.future = this.future.thenCombineAsync(aroundFuture, FinishingTask.INSTANCE, LevelChunkManager.this.executor);
                this.future.whenComplete((chunk, ex) -> {
                    FINISH_RUNNING_UPDATER.compareAndSet(this, 1, 0);
                    if (ex == null && chunk != null) {
                        long key = CloudChunk.key(this.x, this.z);
                        LevelChunkManager.this.level.getUpdateQueue().registerTickContainer(key);
                        chunk.replayDeferredUpdates();
                    }
                });
            }
        }

        private void clear() {
            this.future = this.future.thenApply(chunk -> {
                chunk.clear();
                GENERATION_RUNNING_UPDATER.set(this, 0);
                POPULATION_RUNNING_UPDATER.set(this, 0);
                FINISH_RUNNING_UPDATER.set(this, 0);
                return chunk;
            });
        }
    }
}
