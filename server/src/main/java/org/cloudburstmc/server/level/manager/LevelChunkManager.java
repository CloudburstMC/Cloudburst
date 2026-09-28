package org.cloudburstmc.server.level.manager;

import co.aikar.timings.Timing;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.level.ChunkLoadEvent;
import org.cloudburstmc.api.event.level.ChunkPopulateEvent;
import org.cloudburstmc.api.event.level.ChunkUnloadEvent;
import org.cloudburstmc.server.config.ServerConfig;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.EndDimension;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.LevelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final CloudLevel level;
    private final LevelProvider provider;
    @Getter
    private final ChunkTaskScheduler scheduler;

    /**
     * Uses a mixed version of the public chunk key as the map key. The packed
     * public key hashes as {@code x ^ z} through {@link Long#hashCode()}, which
     * clusters badly for explored chunk coordinates and treeifies buckets.
     */
    private final ConcurrentHashMap<Long, ChunkHolder> chunks = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Long> readyToPromote = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> readyToUnload = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> callbacks = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<SaveRequest> saveQueue = new ConcurrentLinkedQueue<>();
    private final Set<Long> activeSaveChunks = ConcurrentHashMap.newKeySet();
    private final AtomicInteger activeSaves = new AtomicInteger();
    private final int maxActiveSaves;

    /**
     * Counts server ticks to gate the periodic pending-tick flush.
     */
    private int pendingTickSaveTicker = 0;

    public LevelChunkManager(CloudLevel level) {
        this(level, level.getProvider());
    }

    public LevelChunkManager(CloudLevel level, LevelProvider provider) {
        this.level = level;
        ServerConfig.ChunkGeneration chunkGeneration = level.getServer().getConfig().getChunkGeneration();
        this.scheduler = new ChunkTaskScheduler(level, chunkGeneration);
        this.maxActiveSaves = Math.max(1, chunkGeneration.getSaveConcurrency());
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
        for (ChunkHolder loadingChunk : this.chunks.values()) {
            CloudChunk chunk = loadingChunk.getPromotedChunk();
            if (chunk != null) {
                chunks.add(chunk);
            }
        }

        return chunks.build();
    }

    public int getLoadedCount() {
        int count = 0;
        for (ChunkHolder holder : this.chunks.values()) {
            if (holder.getPromotedChunk() != null) {
                count++;
            }
        }

        return count;
    }

    public void addTicket(long chunkKey, ChunkTicketType type, Object identifier) {
        this.getOrCreateChunk(chunkKey).addTicket(type, identifier);
    }

    public void removeTicket(long chunkKey, ChunkTicketType type, Object identifier) {
        ChunkHolder chunk = this.chunks.get(mapKey(chunkKey));
        if (chunk != null && chunk.removeTicket(type, identifier)) {
            this.queueUnload(chunkKey);
        }
    }

    public void addPlayerViewTicket(long chunkKey, Object identifier) {
        this.addTicket(chunkKey, ChunkTicketType.PLAYER_VIEW, identifier);
    }

    public void removePlayerViewTicket(long chunkKey, Object identifier) {
        this.removeTicket(chunkKey, ChunkTicketType.PLAYER_VIEW, identifier);
    }

    /**
     * Get chunk at specified coordinate if it is already loaded.
     *
     * @param key chunk key
     * @return chunk or null
     */
    @Nullable
    public CloudChunk getLoadedChunk(long key) {
        ChunkHolder chunk = this.chunks.get(mapKey(key));
        return chunk == null ? null : chunk.getPromotedChunk();
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

        CloudChunk loadedChunk = this.getLoadedChunk(x, z);
        if (loadedChunk != null) {
            return loadedChunk;
        }

        if (!this.level.getServer().isPrimaryThread()) {
            return this.getChunkFuture(x, z).join();
        }

        long chunkKey = CloudChunk.key(x, z);
        FutureTicket ticket = new FutureTicket(chunkKey);
        ChunkHolder holder = this.getOrCreateChunk(chunkKey);
        holder.addTicket(ChunkTicketType.CHUNK_LOAD, ticket);

        try {
            CloudChunk chunk = holder.getFuture(ChunkStage.FINISHED).join();
            this.promoteChunk(holder);
            return chunk;
        } finally {
            this.removeTicket(chunkKey, ChunkTicketType.CHUNK_LOAD, ticket);
        }
    }

    /**
     * Loads a chunk. Successful completion occurs on the server thread after its load events.
     *
     * @param x chunk x
     * @param z chunk z
     * @return chunk future
     */
    @NonNull
    public CompletableFuture<CloudChunk> getChunkFuture(int x, int z) {
        long chunkKey = CloudChunk.key(x, z);
        FutureTicket ticket = new FutureTicket(chunkKey);
        ChunkHolder holder = this.getOrCreateChunk(chunkKey);
        holder.addTicket(ChunkTicketType.CHUNK_LOAD, ticket);

        CompletableFuture<CloudChunk> result = new CompletableFuture<>();
        result.whenComplete((chunk, throwable) -> this.removeTicket(chunkKey, ChunkTicketType.CHUNK_LOAD, ticket));
        holder.getFuture(ChunkStage.FINISHED).whenComplete((chunk, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
                return;
            }

            this.queueCallback(() -> {
                if (result.isDone()) {
                    return;
                }
                try {
                    this.promoteChunk(holder);
                    result.complete(chunk);
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
        });

        return result;
    }

    @NonNull
    public CompletableFuture<CloudChunk> getChunkFuture(int chunkX, int chunkZ, ChunkStage stage) {
        final long chunkKey = CloudChunk.key(chunkX, chunkZ);
        ChunkHolder chunk = this.getOrCreateChunk(chunkKey);
        return chunk.getFuture(stage);
    }

    private ChunkHolder getOrCreateChunk(long chunkKey) {
        return this.chunks.compute(mapKey(chunkKey), (key, chunk) -> {
            if (chunk == null || chunk.isClosed()) {
                return new ChunkHolder(this, chunkKey);
            }
            return chunk;
        });
    }

    public boolean isChunkLoaded(long hash) {
        ChunkHolder chunk = this.chunks.get(mapKey(hash));
        return chunk != null && chunk.getPromotedChunk() != null;
    }

    public boolean isChunkLoaded(int x, int z) {
        return this.isChunkLoaded(CloudChunk.key(x, z));
    }

    public boolean unloadChunk(long hash) {
        return this.unloadChunk(hash, true);
    }

    public boolean unloadChunk(CloudChunk chunk) {
        return this.unloadChunk(chunk, true);
    }

    public boolean unloadChunk(CloudChunk chunk, boolean save) {
        Preconditions.checkNotNull(chunk, "chunk");
        Preconditions.checkArgument(chunk.getLevel() == this.level, "Chunk is not from this level");
        return this.unloadChunk(CloudChunk.key(chunk.getX(), chunk.getZ()), save);
    }

    public boolean unloadChunk(long chunkKey, boolean save) {
        Preconditions.checkState(this.level.getServer().isPrimaryThread(), "Chunks must be unloaded on the server thread");
        ChunkHolder loadingChunk = this.chunks.get(mapKey(chunkKey));
        if (loadingChunk == null) {
            return false;
        }

        if (tryUnload(chunkKey, loadingChunk, save)) {
            this.chunks.remove(mapKey(chunkKey), loadingChunk);
            return true;
        }

        return false;
    }

    private boolean tryUnload(long chunkKey, ChunkHolder loadingChunk, boolean save) {
        CloudChunk chunk = loadingChunk.getLoadedChunk();
        if (chunk == null) {
            return false;
        }

        if (!loadingChunk.beginUnload()) {
            return false;
        }

        try (Timing ignored = this.level.timings.doChunkUnload.startTiming()) {
            if (loadingChunk.isPromoted()) {
                ChunkUnloadEvent chunkUnloadEvent = new ChunkUnloadEvent(chunk, save);
                this.level.getServer().getEventManager().fire(chunkUnloadEvent);
                save = chunkUnloadEvent.isSaveChunk();
            }

            if (!loadingChunk.close()) {
                return false;
            }

            this.level.unregisterTickContainers(chunkKey);

            CompletableFuture<?> saveFuture = save && chunk.isGenerated() ? this.saveChunk(chunk) : COMPLETED_VOID_FUTURE;
            saveFuture.whenComplete((r, ex) -> {
                LevelChunkManager.this.queueCallback(() -> {
                    if (!LevelChunkManager.this.chunks.containsKey(mapKey(chunkKey))) {
                        LevelChunkManager.this.level.removeTickContainers(chunkKey);
                    }
                    chunk.close();
                });
            });

            return true;
        } finally {
            loadingChunk.endLifecycleCallback();
        }
    }

    public CompletableFuture<Void> saveChunks() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (ChunkHolder loadingChunk : this.chunks.values()) {
            CloudChunk chunk = loadingChunk.getCompleteChunk();
            if (chunk != null) {
                futures.add(saveChunk(chunk));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Void> saveChunk(CloudChunk chunk) {
        Preconditions.checkNotNull(chunk, "chunk");
        Preconditions.checkArgument(chunk.getLevel() == this.level, "Chunk is not from this ChunkManager's Level");
        if (chunk.isDirty()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            this.saveQueue.add(new SaveRequest(chunk, future));
            this.drainSaveQueue();
            return future;
        }

        return COMPLETED_VOID_FUTURE;
    }

    private void drainSaveQueue() {
        int remainingRequests = this.saveQueue.size();
        while (remainingRequests-- > 0) {
            int active = this.activeSaves.get();
            if (active >= this.maxActiveSaves) {
                return;
            }

            SaveRequest request = this.saveQueue.poll();
            if (request == null) {
                return;
            }

            long chunkKey = request.chunkKey();
            if (!this.activeSaveChunks.add(chunkKey)) {
                this.saveQueue.add(request);
                continue;
            }

            if (!this.activeSaves.compareAndSet(active, active + 1)) {
                this.activeSaveChunks.remove(chunkKey);
                this.saveQueue.add(request);
                continue;
            }

            this.provider.saveChunk(request.chunk()).whenComplete((result, throwable) -> {
                try {
                    if (throwable != null) {
                        log.warn("Unable to save chunk", throwable);
                        request.future().completeExceptionally(throwable);
                    } else {
                        request.future().complete(null);
                    }
                } finally {
                    LevelChunkManager.this.activeSaveChunks.remove(chunkKey);
                    LevelChunkManager.this.activeSaves.decrementAndGet();
                    LevelChunkManager.this.drainSaveQueue();
                }
            });
        }
    }

    public void promoteReadyChunks() {
        Preconditions.checkState(this.level.getServer().isPrimaryThread(), "Chunk events must be dispatched on the server thread");
        runCallbacks();
        Long key;
        while ((key = this.readyToPromote.poll()) != null) {
            ChunkHolder loadingChunk = this.chunks.get(mapKey(key));
            if (loadingChunk == null || loadingChunk.isPromoted()) {
                continue;
            }
            this.promoteChunk(loadingChunk);
        }
        runCallbacks();
    }

    private void promoteChunk(ChunkHolder loadingChunk) {
        CloudChunk chunk = loadingChunk.getCompleteChunk();
        if (chunk == null || !loadingChunk.beginPromotion()) {
            return;
        }

        try {
            this.level.registerTickContainers(chunk.getKey());
            chunk.replayRestoredTicks();
            if (loadingChunk.isNewChunk()) {
                EndDimension.onChunkGenerated(this.level, chunk);
            }
            this.level.getServer().getEventManager().fire(new ChunkLoadEvent(chunk, loadingChunk.isNewChunk()));
            if (loadingChunk.isPopulationEventPending()) {
                this.level.getServer().getEventManager().fire(new ChunkPopulateEvent(chunk));
            }
        } finally {
            loadingChunk.endLifecycleCallback();
        }
    }

    private void queueCallback(Runnable callback) {
        this.callbacks.add(callback);
    }

    private void runCallbacks() {
        Runnable callback;
        while ((callback = this.callbacks.poll()) != null) {
            try {
                callback.run();
            } catch (Throwable throwable) {
                log.error("Failed to run chunk callback", throwable);
            }
        }
    }

    public void queueUnload(long chunkKey) {
        this.readyToUnload.add(chunkKey);
    }

    public void queuePromotion(long chunkKey) {
        this.readyToPromote.add(chunkKey);
    }

    private void unloadQueuedChunks() {
        Long chunkKey;
        while ((chunkKey = this.readyToUnload.poll()) != null) {
            ChunkHolder loadingChunk = this.chunks.get(mapKey(chunkKey));
            if (loadingChunk == null) {
                continue;
            }

            this.unloadChunk(chunkKey, true);
        }
    }

    public void tick() {
        promoteReadyChunks();
        unloadQueuedChunks();
        if (this.chunks.isEmpty()) {
            return;
        }

        boolean doTickSave = (++pendingTickSaveTicker >= PENDING_TICK_SAVE_INTERVAL);
        if (doTickSave) {
            pendingTickSaveTicker = 0;
        }

        try (Timing ignored = this.level.timings.doChunkGC.startTiming()) {
            for (var iter = this.chunks.entrySet().iterator(); iter.hasNext(); ) {
                var entry = iter.next();
                ChunkHolder loadingChunk = entry.getValue();
                long chunkKey = loadingChunk.getKey();
                CloudChunk chunk = loadingChunk.getLoadedChunk();
                if (chunk == null) {
                    continue;
                }

                if (doTickSave && loadingChunk.isPromoted() && this.level.areTicksDirty(chunkKey)) {
                    this.provider.savePendingTicks(chunk).exceptionally(throwable -> {
                        log.warn("Failed to incrementally save pending ticks for chunk ({}, {})", chunk.getX(), chunk.getZ(), throwable);
                        return null;
                    });
                }

                if (tryUnload(chunkKey, loadingChunk, true)) {
                    iter.remove();
                }
            }
        }
    }

    public CloudChunk readChunk(ChunkHolder holder) {
        CloudChunk chunk = this.provider.readChunk(new CloudChunkBuilder(holder.getX(), holder.getZ(), this.level));
        if (chunk == null) {
            holder.markNewChunk();
            return new CloudChunk(holder.getX(), holder.getZ(), this.level);
        }

        if (!chunk.isFinished()) {
            holder.markNewChunk();
            if (chunk.isPopulated()) {
                holder.markPopulationComplete();
            }
        }

        chunk.initialize();
        return chunk;
    }

    public void removeHolder(long chunkKey, ChunkHolder holder) {
        this.chunks.remove(mapKey(chunkKey), holder);
    }

    public void loadFailed(ChunkHolder holder, Throwable throwable) {
        log.warn(new ParameterizedMessage("Unable to load chunk ({}, {}) in level {} ",
                holder.getX(), holder.getZ(), this.level.getId()), throwable);
        this.removeHolder(holder.getKey(), holder);
    }

    private record FutureTicket(long chunkKey) {
    }

    private record SaveRequest(CloudChunk chunk, CompletableFuture<Void> future) {
        private long chunkKey() {
            return CloudChunk.key(this.chunk.getX(), this.chunk.getZ());
        }
    }

    private static long mapKey(long chunkKey) {
        long mixed = chunkKey + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
}
