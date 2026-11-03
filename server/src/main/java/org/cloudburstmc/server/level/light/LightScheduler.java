package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Serializes light updates while yielding workers and chunk locks between slices.
 * Initial lighting completes only after the surrounding finished chunks are initialized.
 * Requests may be submitted from any thread. Completion runs on an unspecified thread.
 * A failed update stops the lane and fails outstanding requests.
 */
public class LightScheduler implements AutoCloseable {

    /**
     * Maximum combined propagation steps per slice.
     */
    public static final int SLICE_STEPS = 4096;

    private final Object lock = new Object();
    private final LightLevelView level;
    private final LightRegion region;
    private final Function<Runnable, CompletableFuture<Void>> submit;
    private final Executor retry = CompletableFuture.delayedExecutor(1, TimeUnit.MILLISECONDS);
    private final Deque<Job> queue = new ArrayDeque<>();
    private final Long2ObjectMap<CloudChunk> initialized = new Long2ObjectOpenHashMap<>();
    private boolean scheduled;
    private boolean running;
    private boolean closed;
    private @Nullable Throwable failure;

    /**
     * @param level the lighting settings and finished-chunk lookup
     * @param submit asynchronous submission whose future reports task completion or rejection
     */
    public LightScheduler(LightLevelView level, Function<Runnable, CompletableFuture<Void>> submit) {
        this(level, submit, SLICE_STEPS, null);
    }

    /**
     * @param level      the lighting settings and finished-chunk lookup
     * @param submit     asynchronous submission whose future reports task completion or rejection
     * @param sliceSteps positive propagation budget per slice
     * @param recorder   optional timing callback invoked after chunk locks are released
     */
    public LightScheduler(LightLevelView level, Function<Runnable, CompletableFuture<Void>> submit, int sliceSteps, LightRegion.@Nullable HoldRecorder recorder) {
        this.level = Objects.requireNonNull(level, "level");
        this.submit = Objects.requireNonNull(submit, "submit");
        this.region = new LightRegion(level.registry(), level.minHeight(), level.maxHeight(), level.hasSkyLight(), key -> level.getChunkForLighting(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key)), sliceSteps, recorder);
    }

    /**
     * Requests initial lighting for a finished chunk and its available neighbors.
     * The caller must retain those chunks until completion.
     *
     * @return propagation completion, canceled if the requested chunk unloads
     */
    public CompletableFuture<Void> chunkLoaded(int chunkX, int chunkZ) {
        return this.enqueue(Kind.LOAD, chunkX, chunkZ, LongSet.of());
    }

    /**
     * Requests a boundary refresh after a chunk is removed.
     */
    public void chunkUnloaded(int chunkX, int chunkZ) {
        this.enqueue(Kind.UNLOAD, chunkX, chunkZ, LongSet.of());
    }

    /**
     * Requests light updates for positions packed by {@link LightPositions#pack}.
     * Consecutive unstarted requests share deduplicated work. Canceling the returned
     * future does not discard queued block changes.
     *
     * @return completion of propagation for these changes
     */
    public CompletableFuture<Void> blocksChanged(LongCollection positions) {
        return this.enqueue(Kind.BLOCKS, 0, 0, positions);
    }

    /**
     * Returns whether no requests are queued or running.
     */
    public boolean isIdle() {
        synchronized (this.lock) {
            return !this.scheduled && this.queue.isEmpty();
        }
    }

    /**
     * Checks whether the scheduler has stopped because of a failure.
     *
     * @throws CompletionException if light work or task submission failed
     */
    public void checkFailure() {
        synchronized (this.lock) {
            if (this.failure != null) {
                throw new CompletionException("Level lighting failed", this.failure);
            }
        }
    }

    /**
     * Cancels outstanding requests and rejects new work without waiting for an active slice.
     * An active slice releases its chunk locks before stopping.
     */
    @Override
    public void close() {
        this.stop(new CancellationException("Level lighting is closed"), false);
    }

    private CompletableFuture<Void> enqueue(Kind kind, int chunkX, int chunkZ, LongCollection positions) {
        Job queued;

        synchronized (this.lock) {
            if (this.closed) {
                return CompletableFuture.failedFuture(this.failure != null ? this.failure
                        : new CancellationException("Level lighting is closed"));
            }

            Job tail = this.queue.peekLast();

            if (kind == Kind.BLOCKS && tail != null && tail.kind == Kind.BLOCKS && !tail.started) {
                tail.positions.addAll(positions);
                queued = tail;
            } else {
                queued = new Job(kind, chunkX, chunkZ, positions);
                this.queue.addLast(queued);
            }
        }

        this.schedule(false);
        return queued.kind == Kind.BLOCKS ? queued.completion.copy() : queued.completion;
    }

    private void schedule(boolean delayed) {
        synchronized (this.lock) {
            if (this.closed || this.scheduled || this.queue.isEmpty()) {
                return;
            }

            this.scheduled = true;
        }

        if (delayed) {
            this.retry.execute(this::dispatch);
        } else {
            this.dispatch();
        }
    }

    private void dispatch() {
        synchronized (this.lock) {
            if (this.closed) {
                this.scheduled = false;
                return;
            }
        }

        try {
            this.submit.apply(this::runSlice).whenComplete((ignored, error) -> {
                if (error != null) {
                    this.stop(error, true);
                }
            });
        } catch (Throwable error) {
            this.stop(error, true);
        }
    }

    private void runSlice() {
        Job job;

        synchronized (this.lock) {
            if (this.closed) {
                this.scheduled = false;
                return;
            }

            job = this.queue.peekFirst();
            this.running = true;

            if (job != null) {
                job.started = true;
            }
        }

        if (job == null) {
            synchronized (this.lock) {
                this.scheduled = false;
                this.running = false;
            }

            return;
        }

        boolean busy = false;

        try {
            if (job.completion.isCancelled()) {
                synchronized (this.lock) {
                    this.queue.remove(job);
                }

                return;
            }

            if (job.kind == Kind.LOAD && job.center != null && this.level.getChunkForLighting(job.chunkX, job.chunkZ) != job.center) {
                throw new CancellationException("Chunk unloaded during lighting");
            }

            Deque<Unit> operations = job.operations;

            if (operations == null) {
                operations = this.prepare(job);
                job.operations = operations;
            }

            Unit unit = operations.peekFirst();

            if (unit != null) {
                LightRegion.Step step = unit.operation.advance();
                busy = step == LightRegion.Step.BUSY;

                if (step == LightRegion.Step.COMPLETE) {
                    operations.removeFirst();

                    if (unit.chunk != null) {
                        this.initialized.put(unit.chunk.getKey(), unit.chunk);
                    }
                }
            }

            if (operations.isEmpty()) {
                synchronized (this.lock) {
                    this.queue.remove(job);
                }

                job.completion.complete(null);
            }
        } catch (CancellationException cancelled) {
            synchronized (this.lock) {
                this.queue.remove(job);
            }

            job.completion.completeExceptionally(cancelled);
        } catch (Throwable error) {
            this.stop(error, true);
        } finally {
            synchronized (this.lock) {
                this.scheduled = false;
                this.running = false;

                if (this.closed) {
                    this.initialized.clear();
                }
            }

            this.schedule(busy);
        }
    }

    private Deque<Unit> prepare(Job job) {
        Deque<Unit> operations = new ArrayDeque<>();

        switch (job.kind) {
            case LOAD -> {
                CloudChunk center = this.level.getChunkForLighting(job.chunkX, job.chunkZ);
                if (center == null) {
                    throw new CancellationException("Chunk unloaded before lighting");
                }

                job.center = center;

                for (int x = job.chunkX - 1; x <= job.chunkX + 1; x++) {
                    for (int z = job.chunkZ - 1; z <= job.chunkZ + 1; z++) {
                        CloudChunk chunk = this.level.getChunkForLighting(x, z);
                        if (chunk != null && this.initialized.get(chunk.getKey()) != chunk) {
                            operations.addLast(new Unit(this.region.initialize(x, z), chunk));
                        }
                    }
                }
            }
            case UNLOAD -> {
                this.initialized.keySet().remove(CloudChunk.key(job.chunkX, job.chunkZ));
                operations.addLast(new Unit(this.region.unloaded(job.chunkX, job.chunkZ), null));
            }
            case BLOCKS -> {
                Long2ObjectMap<LongArrayList> groups = new Long2ObjectOpenHashMap<>();

                for (long position : job.positions) {
                    long key = CloudChunk.key(LightPositions.x(position) >> 4, LightPositions.z(position) >> 4);
                    groups.computeIfAbsent(key, ignored -> new LongArrayList()).add(position);
                }

                for (LongArrayList group : groups.values()) {
                    operations.addLast(new Unit(this.region.update(group), null));
                }
            }
        }

        return operations;
    }

    private void stop(Throwable error, boolean failed) {
        List<Job> pending;

        synchronized (this.lock) {
            if (this.closed) {
                return;
            }

            this.closed = true;

            if (failed) {
                this.failure = error;
            }

            pending = new ArrayList<>(this.queue);
            this.queue.clear();

            if (!this.running) {
                this.scheduled = false;
                this.initialized.clear();
            }
        }

        for (Job job : pending) {
            job.completion.completeExceptionally(error);
        }
    }

    private enum Kind {
        LOAD,
        UNLOAD,
        BLOCKS
    }

    private static class Job {
        private final Kind kind;
        private final int chunkX;
        private final int chunkZ;
        private final LongSet positions;
        private final CompletableFuture<Void> completion = new CompletableFuture<>();
        private boolean started;
        private @Nullable Deque<Unit> operations;
        private @Nullable CloudChunk center;

        private Job(Kind kind, int chunkX, int chunkZ, LongCollection positions) {
            this.kind = kind;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.positions = kind == Kind.BLOCKS ? new LongOpenHashSet(positions) : LongSet.of();
        }
    }

    private record Unit(LightRegion.Operation operation, @Nullable CloudChunk chunk) {
    }
}
