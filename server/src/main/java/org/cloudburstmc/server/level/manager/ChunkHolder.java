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

    private final CompletableFuture<CloudChunk> loadFuture;

    private CompletableFuture<CloudChunk> generatedFuture;
    private CompletableFuture<CloudChunk> populatedFuture;
    private CompletableFuture<CloudChunk> finishedFuture;
    private CompletableFuture<CloudChunk> generationWorkFuture;
    private CompletableFuture<CloudChunk> populationWorkFuture;
    private CompletableFuture<CloudChunk> finishingWorkFuture;

    private volatile CloudChunk chunk;

    public ChunkHolder(LevelChunkManager manager, long key) {
        this.manager = manager;
        this.key = key;
        this.x = CloudChunk.fromKeyX(key);
        this.z = CloudChunk.fromKeyZ(key);
        this.loadFuture = manager.getScheduler()
                .scheduleChunkLoad(this.x, this.z, ChunkTaskPriority.NORMAL, () -> manager.readChunk(this));
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

    public void markNewChunk() {
        this.newChunk = true;
    }

    public synchronized void addTicket(ChunkTicketType type, Object identifier) {
        ChunkTicket ticket = new ChunkTicket(type, identifier);
        this.tickets.merge(ticket, 1, Integer::sum);
    }

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

    public synchronized boolean hasTickets() {
        return !this.tickets.isEmpty();
    }

    public synchronized boolean close() {
        if (this.closed) {
            return false;
        }
        this.closed = true;
        this.cancelUnneededWork();
        return true;
    }

    public synchronized CompletableFuture<CloudChunk> getFuture(ChunkStage stage) {
        return switch (stage) {
            case LOADED -> this.loadFuture;
            case GENERATED -> this.generate();
            case POPULATED -> this.populate();
            case FINISHED -> this.finish();
        };
    }

    @Nullable
    public CloudChunk getCompleteChunk() {
        CloudChunk current = this.chunk;
        if (current != null && current.isGenerated() && current.isPopulated() && current.isFinished()) {
            return current;
        }
        return null;
    }

    @Nullable
    public CloudChunk getLoadedChunk() {
        return this.chunk;
    }

    @Nullable
    public CloudChunk getPromotedChunk() {
        if (!this.promoted) {
            return null;
        }
        return this.getCompleteChunk();
    }

    public void promote() {
        this.promoted = true;
    }

    public boolean isIdle() {
        return isDone(this.loadFuture) && isDone(this.generatedFuture) && isDone(this.populatedFuture) && isDone(this.finishedFuture);
    }

    public boolean isComplete() {
        CloudChunk current = this.chunk;
        return current != null && current.isGenerated() && current.isPopulated() && current.isFinished();
    }

    private CompletableFuture<CloudChunk> generate() {
        if (this.generatedFuture == null) {
            this.generatedFuture = this.loadFuture.thenCompose(chunk -> {
                if (chunk.isGenerated()) {
                    return CompletableFuture.completedFuture(chunk);
                }
                this.generationWorkFuture = this.manager.getScheduler()
                        .scheduleGeneration(this.x, this.z, chunk, ChunkTaskPriority.NORMAL);
                return this.generationWorkFuture;
            });
            this.generatedFuture.whenComplete((chunk, throwable) -> {
                if (throwable == null && chunk != null) {
                    this.chunk = chunk;
                }
                this.queueUnloadIfUnneeded();
            });
        }
        return this.generatedFuture;
    }

    private CompletableFuture<CloudChunk> populate() {
        if (this.populatedFuture == null) {
            this.populatedFuture = this.generate().thenCompose(chunk -> {
                if (chunk.isPopulated()) {
                    return CompletableFuture.completedFuture(chunk);
                }

                DependencyBatch dependencies = this.createDependencyBatch(ChunkStage.POPULATED);
                return CompletableFutures.allAsList(dependencies.futures())
                        .thenCompose(neighbors -> {
                            this.populationWorkFuture = this.manager.getScheduler()
                                    .schedulePopulation(this.x, this.z, chunk, neighbors, ChunkTaskPriority.NORMAL);
                            return this.populationWorkFuture;
                        })
                        .whenComplete((result, throwable) -> dependencies.release(this.manager));
            });
            this.populatedFuture.whenComplete((chunk, throwable) -> {
                if (throwable == null && chunk != null) {
                    this.chunk = chunk;
                }
                this.queueUnloadIfUnneeded();
            });
        }
        return this.populatedFuture;
    }

    private CompletableFuture<CloudChunk> finish() {
        if (this.finishedFuture == null) {
            this.finishedFuture = this.populate().thenCompose(chunk -> {
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
                        .whenComplete((result, throwable) -> dependencies.release(this.manager));
            });
            this.finishedFuture.whenComplete((chunk, throwable) -> {
                if (throwable == null && chunk != null) {
                    this.chunk = chunk;
                    this.manager.queuePromotion(this.key);
                }
                this.queueUnloadIfUnneeded();
            });
        }
        return this.finishedFuture;
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
