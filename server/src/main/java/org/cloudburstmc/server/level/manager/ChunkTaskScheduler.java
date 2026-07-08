package org.cloudburstmc.server.level.manager;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.server.config.ServerConfig;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class ChunkTaskScheduler {

    private final TaskQueue storageQueue;
    private final TaskQueue generationQueue;

    public ChunkTaskScheduler(CloudLevel level, ServerConfig.ChunkGeneration config) {
        this.storageQueue = new TaskQueue(
                level.getServer().getLevelManager().getIoExecutor(),
                config.getLoadConcurrency()
        );

        this.generationQueue = new TaskQueue(
                level.getServer().getLevelManager().getGenerationExecutor(),
                config.getGenerationConcurrency()
        );
    }

    public CompletableFuture<CloudChunk> scheduleChunkLoad(int chunkX, int chunkZ, ChunkTaskPriority priority, Supplier<CloudChunk> task) {
        return this.storageQueue.schedule(
                new ChunkTaskIdentifier(ChunkTaskType.READ, CloudChunk.key(chunkX, chunkZ)),
                priority,
                task
        );
    }

    public CompletableFuture<CloudChunk> scheduleGeneration(int chunkX, int chunkZ, CloudChunk chunk, ChunkTaskPriority priority) {
        return this.generationQueue.schedule(
                new ChunkTaskIdentifier(ChunkTaskType.GENERATE, CloudChunk.key(chunkX, chunkZ)),
                priority,
                () -> GenerationTask.INSTANCE.apply(chunk)
        );
    }

    public CompletableFuture<CloudChunk> schedulePopulation(int chunkX, int chunkZ, CloudChunk chunk, List<CloudChunk> neighbors, ChunkTaskPriority priority) {
        List<CloudChunk> neighborSnapshot = ImmutableList.copyOf(neighbors);
        return this.generationQueue.schedule(
                new ChunkTaskIdentifier(ChunkTaskType.POPULATE, CloudChunk.key(chunkX, chunkZ)),
                priority,
                () -> PopulationTask.INSTANCE.apply(chunk, neighborSnapshot)
        );
    }

    public CompletableFuture<CloudChunk> scheduleFinishing(int chunkX, int chunkZ, CloudChunk chunk, List<CloudChunk> neighbors, ChunkTaskPriority priority) {
        List<CloudChunk> neighborSnapshot = ImmutableList.copyOf(neighbors);
        return this.generationQueue.schedule(
                new ChunkTaskIdentifier(ChunkTaskType.FINISH, CloudChunk.key(chunkX, chunkZ)),
                priority,
                () -> FinishingTask.INSTANCE.apply(chunk, neighborSnapshot)
        );
    }

    private static final class TaskQueue {
        private final Executor executor;
        private final int maxActiveTasks;
        private final PriorityQueue<ScheduledTask<?>> pendingTasks = new PriorityQueue<>();
        private final Set<Long> activeAreas = new HashSet<>();
        private final AtomicLong sequence = new AtomicLong();
        private int activeTasks;

        private TaskQueue(Executor executor, int maxActiveTasks) {
            this.executor = executor;
            this.maxActiveTasks = maxActiveTasks > 0 ? maxActiveTasks : defaultMaxActiveTasks(executor);
        }

        private static int defaultMaxActiveTasks(Executor executor) {
            if (executor instanceof ForkJoinPool forkJoinPool) {
                return Math.max(1, forkJoinPool.getParallelism());
            }
            return Math.max(1, Runtime.getRuntime().availableProcessors());
        }

        private <T> CompletableFuture<T> schedule(ChunkTaskIdentifier identifier, ChunkTaskPriority priority, Supplier<T> supplier) {
            CompletableFuture<T> future = new CompletableFuture<>();
            ScheduledTask<T> task = new ScheduledTask<>(
                    identifier,
                    priority,
                    this.sequence.getAndIncrement(),
                    supplier,
                    future
            );

            future.whenComplete((result, throwable) -> {
                if (future.isCancelled()) {
                    this.removePending(task);
                }
            });

            this.addPending(task);
            return future;
        }

        private synchronized void addPending(ScheduledTask<?> task) {
            this.pendingTasks.add(task);
            this.scheduleQueuedTasks();
        }

        private synchronized void removePending(ScheduledTask<?> task) {
            this.pendingTasks.remove(task);
        }

        private synchronized void scheduleQueuedTasks() {
            while (true) {
                if (this.activeTasks >= this.maxActiveTasks) {
                    return;
                }

                ScheduledTask<?> task = this.pollRunnableTask();
                if (task == null) {
                    return;
                }

                this.activeTasks++;
                this.reserve(task);

                try {
                    this.executor.execute(() -> {
                        try {
                            task.run();
                        } finally {
                            this.complete(task);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    this.complete(task);
                    task.completeExceptionally(e);
                }
            }
        }

        private ScheduledTask<?> pollRunnableTask() {
            List<ScheduledTask<?>> skipped = new ArrayList<>();
            ScheduledTask<?> task;
            while ((task = this.pendingTasks.poll()) != null) {
                if (this.canRun(task)) {
                    this.pendingTasks.addAll(skipped);
                    return task;
                }
                skipped.add(task);
            }
            this.pendingTasks.addAll(skipped);
            return null;
        }

        private boolean canRun(ScheduledTask<?> task) {
            int radius = task.identifier().type().getExclusiveRadius();
            int chunkX = task.identifier().chunkX();
            int chunkZ = task.identifier().chunkZ();
            for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                for (int x = chunkX - radius; x <= chunkX + radius; x++) {
                    if (this.activeAreas.contains(CloudChunk.key(x, z))) {
                        return false;
                    }
                }
            }
            return true;
        }

        private void reserve(ScheduledTask<?> task) {
            this.updateActiveArea(task, true);
        }

        private synchronized void complete(ScheduledTask<?> task) {
            this.activeTasks--;
            this.updateActiveArea(task, false);
            this.scheduleQueuedTasks();
        }

        private void updateActiveArea(ScheduledTask<?> task, boolean active) {
            int radius = task.identifier().type().getExclusiveRadius();
            int chunkX = task.identifier().chunkX();
            int chunkZ = task.identifier().chunkZ();
            for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                for (int x = chunkX - radius; x <= chunkX + radius; x++) {
                    long key = CloudChunk.key(x, z);
                    if (active) {
                        this.activeAreas.add(key);
                    } else {
                        this.activeAreas.remove(key);
                    }
                }
            }
        }
    }

    private record ScheduledTask<T>(ChunkTaskIdentifier identifier, ChunkTaskPriority priority, long sequence, Supplier<T> supplier, CompletableFuture<T> future) implements Comparable<ScheduledTask<?>> {
        private void run() {
            if (this.future.isCancelled()) {
                return;
            }

            try {
                this.future.complete(this.supplier.get());
            } catch (Throwable throwable) {
                this.future.completeExceptionally(throwable);
            }
        }

        private void completeExceptionally(Throwable throwable) {
            this.future.completeExceptionally(throwable);
        }

        @Override
        public int compareTo(ScheduledTask<?> other) {
            int priorityCompare = Integer.compare(this.priority.getSortOrder(), other.priority.getSortOrder());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Long.compare(this.sequence, other.sequence);
        }
    }
}
