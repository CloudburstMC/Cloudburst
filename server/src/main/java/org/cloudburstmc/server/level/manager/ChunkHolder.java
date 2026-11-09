package org.cloudburstmc.server.level.manager;

import com.spotify.futures.CompletableFutures;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongConsumer;

/**
 * Tracks retained chunk preparation stages and server-thread lifecycle callbacks.
 * Stage completion does not itself guarantee promotion or event delivery.
 */
@ToString
public final class ChunkHolder {

    private final LevelChunkManager manager;

    @Getter
    private final int x;
    @Getter
    private final int z;
    @Getter
    private final long key;

    private final Map<ChunkTicket, Integer> tickets = new HashMap<>();

    @Getter
    private volatile boolean closed;
    @Getter
    private volatile boolean promoted;
    @Getter
    private volatile boolean newChunk;
    @Getter
    private volatile boolean populationEventPending;
    private boolean lifecycleCallback;

    private final CompletableFuture<CloudChunk> loadFuture;

    private volatile CompletableFuture<CloudChunk> generatedFuture;
    private volatile CompletableFuture<CloudChunk> populatedFuture;
    private volatile CompletableFuture<CloudChunk> finishedFuture;
    private volatile CompletableFuture<CloudChunk> lightedFuture;
    private volatile CompletableFuture<Void> lightingWorkFuture;
    private volatile boolean lightReady;
    private volatile CompletableFuture<CloudChunk> generationWorkFuture;
    private volatile CompletableFuture<CloudChunk> populationWorkFuture;
    private volatile CompletableFuture<CloudChunk> finishingWorkFuture;

    private volatile CloudChunk chunk;

    public ChunkHolder(LevelChunkManager manager, long key) {
        this.manager = manager;
        this.key = key;
        this.x = CloudChunk.fromKeyX(key);
        this.z = CloudChunk.fromKeyZ(key);
        this.loadFuture = manager.getScheduler().scheduleChunkLoad(this.x, this.z, ChunkTaskPriority.NORMAL, () -> manager.readChunk(this));
        this.loadFuture.whenComplete((chunk, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof CancellationException) {
                    manager.removeHolder(key, this);
                    return;
                }

                manager.loadFailed(this, throwable);
            } else {
                this.chunk = chunk;
                this.queueUnloadIfUnneeded();
            }
        });
    }

    /**
     * Marks this load as requiring generation or resumed generation.
     */
    public void markNewChunk() {
        this.newChunk = true;
    }

    /**
     * Records that promotion must deliver the population event.
     */
    public void markPopulationComplete() {
        this.populationEventPending = true;
    }

    /**
     * Adds one retention reference for the ticket.
     */
    public synchronized void addTicket(ChunkTicketType type, Object identifier) {
        ChunkTicket ticket = new ChunkTicket(type, identifier);
        this.tickets.merge(ticket, 1, Integer::sum);
    }

    /**
     * Removes one matching retention reference and cancels unneeded preparation.
     *
     * @return whether a matching ticket was present
     */
    public synchronized boolean removeTicket(ChunkTicketType type, Object identifier) {
        ChunkTicket ticket = new ChunkTicket(type, identifier);
        Integer counter = this.tickets.get(ticket);

        if (counter == null) {
            return false;
        }

        if (counter <= 1) {
            this.tickets.remove(ticket);
        } else {
            this.tickets.put(ticket, counter - 1);
        }

        if (this.tickets.isEmpty()) {
            this.cancelUnneededWork();
        }

        return true;
    }

    /**
     * Returns whether this holder has retention references.
     */
    public synchronized boolean hasTickets() {
        return !this.tickets.isEmpty();
    }

    /**
     * Requests preparation through the given stage.
     * The returned future is shared and does not retain the chunk.
     *
     * @return stage completion, without a server-thread or event-delivery guarantee
     */
    public CompletableFuture<CloudChunk> getFuture(ChunkStage stage) {
        return switch (stage) {
            case LOADED -> this.loadFuture;
            case GENERATED -> this.generate();
            case POPULATED -> this.populate();
            case FINISHED -> this.finish();
            case LIGHTED -> this.light();
        };
    }

    /**
     * Returns the chunk observed after storage loading, or null before loading completes.
     * This does not require generation, lighting or promotion.
     */
    @Nullable
    public CloudChunk getLoadedChunk() {
        return this.chunk;
    }

    /**
     * Returns finished terrain without requiring lighting or promotion, or null when unavailable.
     */
    @Nullable
    public CloudChunk getFinishedChunk() {
        if (this.closed) {
            return null;
        }

        CloudChunk current = this.chunk;
        if (current != null && current.isGenerated() && current.isPopulated() && current.isFinished()) {
            return current;
        }

        return null;
    }

    /**
     * Returns terrain with initial lighting complete, or null when unavailable.
     * The chunk may still await promotion.
     */
    @Nullable
    public CloudChunk getCompleteChunk() {
        return this.lightReady ? this.getFinishedChunk() : null;
    }

    /**
     * Returns the promoted chunk, or null while unavailable or after closure.
     */
    @Nullable
    public CloudChunk getPromotedChunk() {
        if (!this.promoted || this.closed) {
            return null;
        }

        return this.getCompleteChunk();
    }

    /**
     * Returns whether all requested stage futures have completed, including cancellation.
     */
    public boolean isIdle() {
        return isDone(this.loadFuture) && isDone(this.generatedFuture) && isDone(this.populatedFuture) && isDone(this.finishedFuture) && isDone(this.lightedFuture);
    }

    /**
     * Begins a promotion callback for a prepared chunk.
     * A successful call must be paired with {@link #endLifecycleCallback()}.
     *
     * @return whether promotion may proceed
     */
    public synchronized boolean beginPromotion() {
        if (this.closed || this.promoted || this.lifecycleCallback) {
            return false;
        }

        this.promoted = true;
        this.lifecycleCallback = true;
        return true;
    }

    /**
     * Begins unloading only when no preparation or retention prevents it.
     * A successful call must be paired with {@link #endLifecycleCallback()}.
     *
     * @return whether unloading may proceed
     */
    public synchronized boolean beginUnload() {
        if (this.closed || this.lifecycleCallback || !this.isIdle() || !this.tickets.isEmpty()) {
            return false;
        }

        this.lifecycleCallback = true;
        return true;
    }

    /**
     * Ends the current promotion or unload callback.
     */
    public synchronized void endLifecycleCallback() {
        this.lifecycleCallback = false;
    }

    /**
     * Closes an unretained holder and cancels pending preparation where possible.
     * Chunk saving, unload events and storage closure are handled separately.
     *
     * @return whether the holder was closed by this call
     */
    public synchronized boolean close() {
        if (this.closed || !this.tickets.isEmpty()) {
            return false;
        }

        this.closed = true;
        this.cancelUnneededWork();
        return true;
    }

    private CompletableFuture<CloudChunk> generate() {
        CompletableFuture<CloudChunk> result = this.generatedFuture;
        if (result != null) {
            return result;
        }

        synchronized (this) {
            if (this.generatedFuture != null) {
                return this.generatedFuture;
            }

            result = new CompletableFuture<>();
            this.generatedFuture = result;
        }

        result.whenComplete((chunk, throwable) -> {
            if (throwable == null && chunk != null) {
                this.chunk = chunk;
            }

            this.queueUnloadIfUnneeded();
        });

        CompletableFuture<CloudChunk> pipeline;
        try {
            pipeline = this.loadFuture.thenCompose(chunk -> {
                if (chunk.isGenerated()) {
                    return CompletableFuture.completedFuture(chunk);
                }

                this.generationWorkFuture = this.manager.getScheduler().scheduleGeneration(this.x, this.z, chunk, ChunkTaskPriority.NORMAL);
                return this.generationWorkFuture;
            });
        } catch (RuntimeException exception) {
            result.completeExceptionally(exception);
            return result;
        }

        completeFrom(result, pipeline);
        return result;
    }

    private CompletableFuture<CloudChunk> populate() {
        CompletableFuture<CloudChunk> result = this.populatedFuture;
        if (result != null) {
            return result;
        }

        synchronized (this) {
            if (this.populatedFuture != null) {
                return this.populatedFuture;
            }

            result = new CompletableFuture<>();
            this.populatedFuture = result;
        }

        result.whenComplete((chunk, throwable) -> {
            if (throwable == null && chunk != null) {
                this.chunk = chunk;
            }

            this.queueUnloadIfUnneeded();
        });

        CompletableFuture<CloudChunk> pipeline;
        try {
            pipeline = this.generate().thenCompose(chunk -> {
                if (chunk.isPopulated()) {
                    return CompletableFuture.completedFuture(chunk);
                }

                DependencyBatch dependencies = this.createDependencyBatch(ChunkStage.POPULATED);
                return CompletableFutures.allAsList(dependencies.futures())
                        .thenCompose(neighbors -> {
                            this.populationWorkFuture = this.manager.getScheduler()
                                    .schedulePopulation(this.x, this.z, chunk, neighbors, ChunkTaskPriority.NORMAL);
                            return this.populationWorkFuture.thenApply(populatedChunk -> {
                                this.markPopulationComplete();
                                return populatedChunk;
                            });
                        })
                        .whenComplete((populatedChunk, throwable) -> dependencies.release(this.manager));
            });
        } catch (RuntimeException exception) {
            result.completeExceptionally(exception);
            return result;
        }

        completeFrom(result, pipeline);
        return result;
    }

    private CompletableFuture<CloudChunk> finish() {
        CompletableFuture<CloudChunk> result = this.finishedFuture;
        if (result != null) {
            return result;
        }

        synchronized (this) {
            if (this.finishedFuture != null) {
                return this.finishedFuture;
            }

            result = new CompletableFuture<>();
            this.finishedFuture = result;
        }

        result.whenComplete((chunk, throwable) -> {
            if (throwable == null && chunk != null) {
                this.chunk = chunk;
            }

            this.queueUnloadIfUnneeded();
        });

        CompletableFuture<CloudChunk> pipeline;

        try {
            pipeline = this.populate().thenCompose(chunk -> {
                if (chunk.isFinished()) {
                    return CompletableFuture.completedFuture(chunk);
                }

                DependencyBatch dependencies = this.createDependencyBatch(ChunkStage.FINISHED);
                return CompletableFutures.allAsList(dependencies.futures())
                        .thenCompose(neighbors -> {
                            this.finishingWorkFuture = this.manager.getScheduler()
                                    .scheduleFinishing(this.x, this.z, chunk, neighbors, ChunkTaskPriority.HIGH);
                            return this.finishingWorkFuture;
                        })
                        .whenComplete((finishedChunk, throwable) -> dependencies.release(this.manager));
            });
        } catch (RuntimeException exception) {
            result.completeExceptionally(exception);
            return result;
        }

        completeFrom(result, pipeline);
        return result;
    }

    private CompletableFuture<CloudChunk> light() {
        CompletableFuture<CloudChunk> result = this.lightedFuture;

        if (result != null) {
            return result;
        }

        synchronized (this) {
            if (this.lightedFuture != null) {
                return this.lightedFuture;
            }

            result = new CompletableFuture<>();
            this.lightedFuture = result;
        }

        result.whenComplete((chunk, error) -> {
            if (error == null && !this.closed) {
                this.manager.queuePromotion(this.key);
            }

            this.queueUnloadIfUnneeded();
        });

        CompletableFuture<CloudChunk> pipeline;

        try {
            pipeline = this.finish().thenCompose(chunk -> {
                DependencyBatch dependencies = this.createDependencyBatch(ChunkStage.LIGHTED);

                return CompletableFutures.allAsList(dependencies.futures())
                        .thenCompose(neighbours -> {
                            if (this.closed || this.lightedFuture.isCancelled()) {
                                return CompletableFuture.failedFuture(new CancellationException("Chunk lighting cancelled"));
                            }

                            this.lightingWorkFuture = this.manager.initializeChunkLight(chunk);
                            return this.lightingWorkFuture.thenApply(ignored -> {
                                this.lightReady = true;
                                return chunk;
                            });
                        })
                        .whenComplete((lit, error) -> dependencies.release(this.manager));
            });
        } catch (RuntimeException error) {
            result.completeExceptionally(error);
            return result;
        }

        completeFrom(result, pipeline);
        return result;
    }

    private DependencyBatch createDependencyBatch(ChunkStage stage) {
        List<CompletableFuture<CloudChunk>> futures = new ArrayList<>(8);
        LongList dependencyKeys = new LongArrayList(8);
        GenerationDependency dependency = new GenerationDependency(this.key, stage);

        for (int z = this.z - 1, maxZ = this.z + 1; z <= maxZ; z++) {
            for (int x = this.x - 1, maxX = this.x + 1; x <= maxX; x++) {
                if (x == this.x && z == this.z) {
                    continue;
                }

                long dependencyKey = CloudChunk.key(x, z);
                this.manager.addTicket(dependencyKey, ChunkTicketType.GENERATION_DEPENDENCY, dependency);
                dependencyKeys.add(dependencyKey);
                futures.add(this.manager.getChunkFuture(x, z, stage.previous()));
            }
        }

        return new DependencyBatch(dependency, dependencyKeys, futures);
    }

    private void queueUnloadIfUnneeded() {
        if (!this.hasTickets()) {
            this.manager.queueUnload(this.key);
        }
    }

    private void cancelUnneededWork() {
        if (this.promoted || this.hasTickets()) {
            return;
        }

        if (this.chunk == null) {
            cancel(this.loadFuture);
        }

        cancel(this.finishingWorkFuture);
        cancel(this.lightingWorkFuture);
        cancel(this.lightedFuture);
        cancel(this.finishedFuture);
        cancel(this.populationWorkFuture);
        cancel(this.populatedFuture);
        cancel(this.generationWorkFuture);
        cancel(this.generatedFuture);
    }

    private static void cancel(@Nullable CompletableFuture<?> future) {
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    private static boolean isDone(@Nullable CompletableFuture<?> future) {
        return future == null || future.isDone();
    }

    private static <T> void completeFrom(CompletableFuture<T> target, CompletableFuture<T> source) {
        source.whenComplete((value, throwable) -> {
            if (throwable == null) {
                target.complete(value);
            } else {
                target.completeExceptionally(throwable);
            }
        });
    }

    private record ChunkTicket(ChunkTicketType type, Object identifier) {
    }

    private record GenerationDependency(long sourceChunkKey, ChunkStage stage) {
    }

    private record DependencyBatch(GenerationDependency dependency, LongList keys, List<CompletableFuture<CloudChunk>> futures) {
        private void release(LevelChunkManager manager) {
            this.keys.forEach((LongConsumer) key -> manager.removeTicket(key, ChunkTicketType.GENERATION_DEPENDENCY, this.dependency));
        }
    }
}
