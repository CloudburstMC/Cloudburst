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
import org.cloudburstmc.server.configuration.GlobalConfiguration;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.EndDimension;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.provider.LevelProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates chunk loading, generation, retention, promotion and persistence for one level.
 *
 * <p>Loaded-chunk queries expose complete, promoted chunks. Promotion initializes a chunk
 * and dispatches its load and pending population events on the server thread.
 * Generation-stage completion alone does not guarantee promotion.
 *
 * <p>Tickets retain chunks independently of generation requests. Callers must hold their
 * own ticket to retain a chunk after loading. Queued completions require the server thread
 * to process {@link #promoteReadyChunks()} or {@link #tick()}.
 */
@Log4j2
public class LevelChunkManager {
    private static final CompletableFuture<Void> COMPLETED_VOID_FUTURE = CompletableFuture.completedFuture(null);

    /**
     * Manager ticks between asynchronous save attempts for dirty pending-tick containers
     * on promoted chunks. Independent of full chunk saves and subject to provider support.
     */
    private static final int PENDING_TICK_SAVE_INTERVAL = 300;

    private final CloudLevel level;
    private final LevelProvider provider;
    @Getter
    private final ChunkTaskScheduler scheduler;

    /**
     * Uses mixed chunk keys to avoid the coordinate collisions of the packed key's
     * {@code x ^ z} hash from {@link Long#hashCode(long)}.
     */
    private final ConcurrentHashMap<Long, ChunkHolder> chunks = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Long> readyToPromote = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> readyToUnload = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> callbacks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SaveRequest> saveQueue = new ConcurrentLinkedQueue<>();
    private final Set<Long> activeSaveChunks = ConcurrentHashMap.newKeySet();
    private final AtomicInteger activeSaves = new AtomicInteger();
    private final int maxActiveSaves;

    private int pendingTickSaveTicker;

    /**
     * Creates a manager using the level's provider and chunk-generation configuration.
     */
    public LevelChunkManager(CloudLevel level) {
        this(level, level.getProvider());
    }

    /**
     * Creates a manager using the supplied provider and the level's chunk-generation configuration.
     * The caller retains responsibility for closing the provider.
     */
    public LevelChunkManager(CloudLevel level, LevelProvider provider) {
        this.level = level;
        GlobalConfiguration.ChunkGeneration chunkGeneration = level.getServer().getConfig().getChunkGeneration();
        this.scheduler = new ChunkTaskScheduler(level, chunkGeneration);
        this.maxActiveSaves = Math.max(1, chunkGeneration.getSaveConcurrency());
        this.provider = provider;
    }

    /**
     * Collects the complete, promoted chunks currently visible to this manager.
     * Concurrent holder changes are not captured as an atomic snapshot.
     *
     * @return an immutable set that does not track later loads or unloads
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

    /**
     * Counts complete, promoted chunks without taking an atomic snapshot of the holders.
     */
    public int getLoadedCount() {
        int count = 0;
        for (ChunkHolder holder : this.chunks.values()) {
            if (holder.getPromotedChunk() != null) {
                count++;
            }
        }

        return count;
    }

    /**
     * Returns a complete, promoted chunk without starting a load.
     *
     * @param key packed key from {@link CloudChunk#key(int, int)}
     * @return the chunk, or {@code null} if no open holder exposes a promoted chunk
     */
    @Nullable
    public CloudChunk getLoadedChunk(long key) {
        ChunkHolder chunk = this.chunks.get(mapKey(key));
        return chunk == null ? null : chunk.getPromotedChunk();
    }

    /**
     * Returns a complete, promoted chunk without starting a load.
     *
     * @param x chunk x
     * @param z chunk z
     * @return the chunk, or {@code null} if no open holder exposes a promoted chunk
     */
    @Nullable
    public CloudChunk getLoadedChunk(int x, int z) {
        return getLoadedChunk(CloudChunk.key(x, z));
    }

    /**
     * Tests whether an open holder exposes a complete, promoted chunk at the packed key.
     */
    public boolean isChunkLoaded(long hash) {
        ChunkHolder chunk = this.chunks.get(mapKey(hash));
        return chunk != null && chunk.getPromotedChunk() != null;
    }

    /**
     * Tests whether an open holder exposes a complete, promoted chunk at these coordinates.
     */
    public boolean isChunkLoaded(int x, int z) {
        return this.isChunkLoaded(CloudChunk.key(x, z));
    }

    /**
     * Returns a loaded chunk or blocks for the finished generation stage and promotion.
     * On the server thread, promotion is performed inline. Other threads wait for the
     * queued completion processed by {@link #promoteReadyChunks()}.
     *
     * <p>All {@link ForkJoinWorkerThread} callers are rejected to avoid blocking a worker
     * needed by generation. Loading uses a temporary ticket, not lasting retention.
     *
     * @param x chunk x
     * @param z chunk z
     * @return chunk
     * @throws IllegalStateException if called from a fork-join worker
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
     * Loads a chunk and completes successfully on the server thread after attempting promotion.
     * A newly promoted chunk receives load and pending population events before completion.
     * Requires the server thread to process queued promotions.
     *
     * <p>The temporary load ticket is released when the returned future completes or is
     * canceled. Cancellation does not guarantee that shared generation stops.
     * Retention after completion requires a separate ticket.
     *
     * @param x chunk x
     * @param z chunk z
     * @return a separate future for this load request
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

    /**
     * Requests a generation stage without adding a retention ticket or waiting for promotion.
     * Even {@link ChunkStage#FINISHED} completion does not guarantee server-thread initialization
     * or event dispatch. Callers must manage retention separately. The returned future is the
     * holder's shared stage future, not an independent load request.
     *
     * @param chunkX chunk x
     * @param chunkZ chunk z
     * @param stage  required generation stage
     * @return the shared stage future, with no server-thread completion guarantee
     */
    @NonNull
    public CompletableFuture<CloudChunk> getChunkFuture(int chunkX, int chunkZ, ChunkStage stage) {
        final long chunkKey = CloudChunk.key(chunkX, chunkZ);
        ChunkHolder chunk = this.getOrCreateChunk(chunkKey);
        return chunk.getFuture(stage);
    }

    /**
     * Retains a holder, creating it and starting its provider read if necessary.
     * Does not request generation or promotion. Repeated tickets with equal type and
     * identifier are counted and require matching removals.
     *
     * @param chunkKey   packed key from {@link CloudChunk#key(int, int)}
     * @param type       retention category
     * @param identifier owner identity, compared by equality
     */
    public void addTicket(long chunkKey, ChunkTicketType type, Object identifier) {
        this.getOrCreateChunk(chunkKey).addTicket(type, identifier);
    }

    /**
     * Removes one matching ticket reference and queues an unload eligibility check if found.
     * Removing the last ticket may cancel unfinished work on an unpromoted holder.
     * A missing holder or ticket is ignored.
     */
    public void removeTicket(long chunkKey, ChunkTicketType type, Object identifier) {
        ChunkHolder chunk = this.chunks.get(mapKey(chunkKey));
        if (chunk != null && chunk.removeTicket(type, identifier)) {
            this.queueUnload(chunkKey);
        }
    }

    /**
     * Adds a {@link ChunkTicketType#PLAYER_VIEW} retention ticket without requesting generation.
     */
    public void addPlayerViewTicket(long chunkKey, Object identifier) {
        this.addTicket(chunkKey, ChunkTicketType.PLAYER_VIEW, identifier);
    }

    /**
     * Removes one matching {@link ChunkTicketType#PLAYER_VIEW} ticket reference.
     */
    public void removePlayerViewTicket(long chunkKey, Object identifier) {
        this.removeTicket(chunkKey, ChunkTicketType.PLAYER_VIEW, identifier);
    }

    /**
     * Attempts to unload by packed key with saving requested. See {@link #unloadChunk(long, boolean)}.
     */
    public boolean unloadChunk(long hash) {
        return this.unloadChunk(hash, true);
    }

    /**
     * Attempts to unload at the chunk's coordinates with saving requested.
     * See {@link #unloadChunk(CloudChunk, boolean)}.
     */
    public boolean unloadChunk(CloudChunk chunk) {
        return this.unloadChunk(chunk, true);
    }

    /**
     * Attempts to unload at the supplied chunk's coordinates after validating level ownership.
     * See {@link #unloadChunk(long, boolean)} for eligibility and completion semantics.
     */
    public boolean unloadChunk(CloudChunk chunk, boolean save) {
        Preconditions.checkNotNull(chunk, "chunk");
        Preconditions.checkArgument(chunk.getLevel() == this.level, "Chunk is not from this level");
        return this.unloadChunk(CloudChunk.key(chunk.getX(), chunk.getZ()), save);
    }

    /**
     * Attempts to unload an idle, ticket-free chunk on the server thread.
     * Promoted chunks receive an unload event that can change the save preference.
     * Tickets added during that event can prevent unloading.
     *
     * <p>Returns before saving and deferred cleanup finish. Cleanup requires a later
     * call to {@link #promoteReadyChunks()} or {@link #tick()} and runs even if saving fails.
     *
     * @param chunkKey packed chunk key
     * @param save     initial save preference for the unload event
     * @return {@code true} if removed from this manager, regardless of the eventual save result
     * @throws IllegalStateException if not called on the server thread
     */
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

    /**
     * Submits saves for complete chunks currently observed, including unpromoted chunks.
     * Does not wait for generation or include holders already removed for unloading.
     *
     * @return future for these save requests, failing if any request fails
     */
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

    /**
     * Queues a full save if the chunk is dirty when called, or returns a completed future.
     * Provider saves are concurrency-limited and serialized by chunk coordinates. This does
     * not capture a snapshot at submission or promise to persist mutations made during a save.
     *
     * @param chunk chunk belonging to this level, regardless of whether this manager holds it
     * @return future for the provider save, with no server-thread completion guarantee
     */
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

    /**
     * Drains callbacks, promotes queued complete chunks, then drains callbacks again.
     * Must run on the server thread. Also processes deferred chunk closure after unloads.
     */
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

    /**
     * Queues an unload eligibility check for {@link #tick()}. Does not release tickets or
     * force an unload. May be called from a worker thread.
     */
    public void queueUnload(long chunkKey) {
        this.readyToUnload.add(chunkKey);
    }

    /**
     * Queues a promotion attempt for {@link #promoteReadyChunks()} without requesting any
     * generation work. May be called from a worker thread.
     */
    public void queuePromotion(long chunkKey) {
        this.readyToPromote.add(chunkKey);
    }

    /**
     * Processes promotions, callbacks and queued unloads on the server thread, then checks
     * remaining loaded holders for unloading. Periodically submits independent pending-tick
     * saves for promoted chunks with dirty tick containers. Does not wait for saves.
     */
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
            for (Iterator<Map.Entry<Long, ChunkHolder>> iter = this.chunks.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry<Long, ChunkHolder> entry = iter.next();
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

    /**
     * Reads persisted state or creates an empty chunk for the scheduler's load request.
     * Records whether generation and population events remain applicable.
     * Does not initialize or promote the chunk.
     */
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

        return chunk;
    }

    /**
     * Removes a failed or canceled holder only if it is still mapped at the key.
     * Does not perform unload events, saving or chunk closure.
     */
    public void removeHolder(long chunkKey, ChunkHolder holder) {
        this.chunks.remove(mapKey(chunkKey), holder);
    }

    /**
     * Logs a holder's provider-load failure and removes that holder without unloading it.
     * Called from the load future's completion thread.
     */
    public void loadFailed(ChunkHolder holder, Throwable throwable) {
        log.warn(new ParameterizedMessage("Unable to load chunk ({}, {}) in world '{}'", holder.getX(), holder.getZ(), this.level.getId()), throwable);
        this.removeHolder(holder.getKey(), holder);
    }

    private ChunkHolder getOrCreateChunk(long chunkKey) {
        return this.chunks.compute(mapKey(chunkKey), (key, chunk) -> {
            if (chunk == null || chunk.isClosed()) {
                return new ChunkHolder(this, chunkKey);
            }
            return chunk;
        });
    }

    private void promoteChunk(ChunkHolder loadingChunk) {
        CloudChunk chunk = loadingChunk.getCompleteChunk();
        if (chunk == null || !loadingChunk.beginPromotion()) {
            return;
        }

        try {
            this.level.registerTickContainers(chunk.getKey());
            chunk.initialize();
            chunk.replayRestoredTicks();
            if (loadingChunk.isNewChunk()) {
                EndDimension.onChunkGenerated(this.level, chunk);
            }
            this.level.initializeChunkLight(chunk);
            this.level.getServer().getEventManager()
                    .fire(new ChunkLoadEvent(chunk, loadingChunk.isNewChunk()));
            if (loadingChunk.isPopulationEventPending()) {
                this.level.getServer().getEventManager().fire(new ChunkPopulateEvent(chunk));
            }
        } finally {
            loadingChunk.endLifecycleCallback();
        }
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
            this.level.updateChunkLightBoundary(chunk);

            CompletableFuture<?> saveFuture = save && chunk.isGenerated()
                    ? this.saveChunk(chunk) : COMPLETED_VOID_FUTURE;
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

    private static long mapKey(long chunkKey) {
        long mixed = chunkKey + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    private record FutureTicket(long chunkKey) {
    }

    private record SaveRequest(CloudChunk chunk, CompletableFuture<Void> future) {
        private long chunkKey() {
            return CloudChunk.key(this.chunk.getX(), this.chunk.getZ());
        }
    }
}
