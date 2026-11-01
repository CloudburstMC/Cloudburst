package org.cloudburstmc.server.level.light;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.server.level.chunk.CloudChunk;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * Queues light work per level and runs it off the caller's thread.
 *
 * <p>{@code chunkLoaded}, {@code chunkUnloaded} and {@code blocksChanged} may
 * be called from any thread. {@code isIdle} is true only when nothing is queued
 * and no runner is accepted or running.
 *
 * <p>Submissions only enqueue and hand one runner to {@code submit}; the
 * runner processes one work item per run, in FIFO order, then resubmits itself
 * while the FIFO is non-empty. At most one runner is in flight, so units never
 * overlap. Consecutive block-change items are merged. A unit that throws is
 * logged and dropped while the lane continues. When {@code submit} throws
 * {@link RejectedExecutionException}, pending work is dropped and the lane goes
 * idle, accepting later submissions. When {@code submit} throws anything else,
 * the in-flight flag is cleared with queued work kept, the throwable
 * propagates, and the next submission restarts the lane.
 *
 * <p>Chunk loads initialize, unloads refresh the ring, and block changes are
 * grouped by chunk with one update per group, each locking only that chunk's
 * 3x3. Every unit propagates in slices through {@link LightRegion}; each lock
 * hold is recorded in a bounded ring for the benchmark.
 *
 * <p>{@link LightLevelView#loadedChunk} must stay callable from the lane.
 */
@Log4j2
public final class LightScheduler {

    /**
     * The propagation steps per lock hold.
     */
    public static final int SLICE_STEPS = 4096;

    private static final int HOLD_CAPACITY = 4096;

    private final LightRegion region;
    private final Consumer<Runnable> submit;

    private final Object laneLock = new Object();
    private final Deque<WorkItem> queue = new ArrayDeque<>();
    private boolean runnerInFlight;

    private final Object holdLock = new Object();
    private final long[] holds = new long[HOLD_CAPACITY];
    private int holdNext;
    private long holdTotal;

    /**
     * @param level  the level state the lane reads
     * @param submit hands each lane runnable to another thread; it must not run
     *               the runnable synchronously on the calling thread, since each
     *               run resubmits from within the run and synchronous execution
     *               would nest a frame per queued item
     */
    public LightScheduler(LightLevelView level, Consumer<Runnable> submit) {
        this(level, submit, SLICE_STEPS);
    }

    /**
     * @param level      the level state the lane reads
     * @param submit     hands each lane runnable to another thread; it must not
     *                   run the runnable synchronously on the calling thread,
     *                   since each run resubmits from within the run and
     *                   synchronous execution would nest a frame per queued item
     * @param sliceSteps the combined propagation step budget per lock hold
     */
    LightScheduler(LightLevelView level, Consumer<Runnable> submit, int sliceSteps) {
        Preconditions.checkNotNull(level, "level");
        Preconditions.checkNotNull(submit, "submit");
        Preconditions.checkArgument(sliceSteps > 0, "sliceSteps must be positive");
        this.region = new LightRegion(level.registry(), level.minHeight(), level.maxHeight(), level.hasSkyLight(),
                key -> level.loadedChunk(CloudChunk.fromKeyX(key), CloudChunk.fromKeyZ(key)),
                sliceSteps, this::recordHold);
        this.submit = submit;
    }

    /**
     * Queues lighting for a loaded chunk.
     *
     * @param chunkX the chunk x
     * @param chunkZ the chunk z
     */
    public void chunkLoaded(int chunkX, int chunkZ) {
        synchronized (this.laneLock) {
            this.queue.addLast(WorkItem.load(chunkX, chunkZ));
        }
        submitRunner();
    }

    /**
     * Queues a boundary refresh around an unloaded chunk.
     *
     * @param chunkX the chunk x
     * @param chunkZ the chunk z
     */
    public void chunkUnloaded(int chunkX, int chunkZ) {
        synchronized (this.laneLock) {
            this.queue.addLast(WorkItem.unload(chunkX, chunkZ));
        }
        submitRunner();
    }

    /**
     * Queues repropagation from the changed packed positions.
     *
     * @param positions the packed positions
     */
    public void blocksChanged(LongCollection positions) {
        if (positions.isEmpty()) {
            return;
        }
        synchronized (this.laneLock) {
            WorkItem tail = this.queue.peekLast();
            if (tail != null && tail.kind == WorkItem.Kind.BLOCKS) {
                tail.positions.addAll(positions);
            } else {
                this.queue.addLast(WorkItem.blocks(positions));
            }
        }
        submitRunner();
    }

    /**
     * Returns whether no work is queued or running.
     *
     * @return true when idle
     */
    public boolean isIdle() {
        synchronized (this.laneLock) {
            return !this.runnerInFlight && this.queue.isEmpty();
        }
    }

    /**
     * Returns the longest recorded lock hold, or 0 when none was recorded.
     *
     * @return the maximum hold in nanoseconds
     */
    public long maxHoldNanos() {
        synchronized (this.holdLock) {
            long max = 0;
            int count = (int) Math.min(this.holdTotal, HOLD_CAPACITY);
            for (int i = 0; i < count; i++) {
                max = Math.max(max, this.holds[i]);
            }
            return max;
        }
    }

    /**
     * Returns the recorded lock holds, oldest first, bounded to the last
     * {@value #HOLD_CAPACITY}.
     *
     * @return the hold samples in nanoseconds
     */
    public long[] holdNanosSamples() {
        synchronized (this.holdLock) {
            int count = (int) Math.min(this.holdTotal, HOLD_CAPACITY);
            long[] samples = new long[count];
            int start = this.holdNext - count;
            if (start < 0) {
                start += HOLD_CAPACITY;
            }
            for (int i = 0; i < count; i++) {
                samples[i] = this.holds[(start + i) % HOLD_CAPACITY];
            }
            return samples;
        }
    }

    /**
     * Runs a hook once after the next propagation slice, without holding chunk
     * locks.
     *
     * @param hook the hook, or null to clear it
     */
    void afterSliceForTest(@Nullable Runnable hook) {
        this.region.afterSliceForTest(hook);
    }

    private void submitRunner() {
        synchronized (this.laneLock) {
            if (this.runnerInFlight) {
                return;
            }
            this.runnerInFlight = true;
        }
        try {
            this.submit.accept(this::runLane);
        } catch (RejectedExecutionException e) {
            synchronized (this.laneLock) {
                this.queue.clear();
                this.runnerInFlight = false;
            }
        } catch (Throwable e) {
            synchronized (this.laneLock) {
                this.runnerInFlight = false;
            }
            throw e;
        }
    }

    private void runLane() {
        WorkItem item;
        synchronized (this.laneLock) {
            item = this.queue.pollFirst();
            if (item == null) {
                this.runnerInFlight = false;
                return;
            }
        }
        try {
            if (item.kind == WorkItem.Kind.BLOCKS) {
                synchronized (this.laneLock) {
                    WorkItem next;
                    while ((next = this.queue.peekFirst()) != null && next.kind == WorkItem.Kind.BLOCKS) {
                        this.queue.pollFirst();
                        item.positions.addAll(next.positions);
                    }
                }
            }
            process(item);
        } catch (Throwable e) {
            log.error("Dropping light work after failure", e);
        } finally {
            boolean idle;
            synchronized (this.laneLock) {
                idle = this.queue.isEmpty();
                if (idle) {
                    this.runnerInFlight = false;
                }
            }
            if (!idle) {
                try {
                    this.submit.accept(this::runLane);
                } catch (RejectedExecutionException e) {
                    synchronized (this.laneLock) {
                        this.queue.clear();
                        this.runnerInFlight = false;
                    }
                } catch (Throwable e) {
                    synchronized (this.laneLock) {
                        this.runnerInFlight = false;
                    }
                    throw e;
                }
            }
        }
    }

    private void process(WorkItem item) {
        switch (item.kind) {
            case LOAD -> runUnit("initialize",
                    () -> this.region.initialize(item.chunkX, item.chunkZ));
            case UNLOAD -> runUnit("unloaded",
                    () -> this.region.unloaded(item.chunkX, item.chunkZ));
            case BLOCKS -> {
                Long2ObjectMap<LongArrayList> groups = new Long2ObjectOpenHashMap<>();
                for (long packed : item.positions) {
                    long key = CloudChunk.key(LightPositions.x(packed) >> 4, LightPositions.z(packed) >> 4);
                    LongArrayList group = groups.get(key);
                    if (group == null) {
                        group = new LongArrayList();
                        groups.put(key, group);
                    }
                    group.add(packed);
                }
                long[] keys = groups.keySet().toLongArray();
                Arrays.sort(keys);
                for (long key : keys) {
                    LongArrayList group = groups.get(key);
                    runUnit("update", () -> this.region.update(group));
                }
            }
        }
    }

    private void runUnit(String name, Runnable unit) {
        try {
            unit.run();
        } catch (Throwable e) {
            log.error("Dropping light unit {} after failure", name, e);
        }
    }

    private void recordHold(long holdNanos) {
        synchronized (this.holdLock) {
            this.holds[this.holdNext] = holdNanos;
            this.holdNext = (this.holdNext + 1) % HOLD_CAPACITY;
            this.holdTotal++;
        }
    }

    private static final class WorkItem {

        enum Kind {
            LOAD,
            UNLOAD,
            BLOCKS
        }

        final Kind kind;
        final int chunkX;
        final int chunkZ;
        final LongArrayList positions;

        private WorkItem(Kind kind, int chunkX, int chunkZ, LongArrayList positions) {
            this.kind = kind;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.positions = positions;
        }

        static WorkItem load(int chunkX, int chunkZ) {
            return new WorkItem(Kind.LOAD, chunkX, chunkZ, new LongArrayList());
        }

        static WorkItem unload(int chunkX, int chunkZ) {
            return new WorkItem(Kind.UNLOAD, chunkX, chunkZ, new LongArrayList());
        }

        static WorkItem blocks(LongCollection positions) {
            return new WorkItem(Kind.BLOCKS, 0, 0, new LongArrayList(positions));
        }
    }
}
