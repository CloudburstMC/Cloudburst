package org.cloudburstmc.server.scheduler;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.utils.BlockUpdateEntry;

import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * World-level orchestrator for scheduled block ticks.
 *
 * <h2>Container lifecycle</h2>
 * <p>Containers survive across unload/reload cycles. {@link #unregisterTickContainer}
 * disconnects the callback and removes the chunk from the fast-path timing map,
 * leaving the container dormant with its pending data intact.
 * {@link #registerTickContainer} re-wires it on reload.
 * {@link #removeTickContainer} permanently discards the container after the
 * chunk's tick data has been saved to disk.
 *
 * <h2>Three-phase tick</h2>
 * <ol>
 *   <li>{@link #sortContainersToTick}: selects containers with due ticks that
 *       pass {@code tickCheck} and inserts them into a priority queue ordered by
 *       {@link BlockUpdateEntry#INTRA_TICK_ORDER}.</li>
 *   <li>{@link #drainContainers} + {@link #drainFromCurrentContainer}: pulls
 *       entries across containers in global priority order.</li>
 *   <li>{@link #runCollectedTicks}: executes collected entries.</li>
 *   <li>{@link #cleanupAfterTick}: resets scratch structures.</li>
 * </ol>
 *
 * <h2>Locking model</h2>
 * <p>Uses a {@link StampedLock} for fine-grained concurrency:
 * <ul>
 *   <li>Read-only queries ({@link #contains}, {@link #willTickThisTick},
 *       {@link #isDirty}, {@link #getPendingBlockUpdates}) acquire optimistic
 *       or shared read stamps.</li>
 *   <li>All mutating operations ({@link #add}, {@link #remove}, {@link #tick},
 *       {@link #markSaved}, {@link #clearArea}, {@link #copyArea}) acquire
 *       exclusive write stamps.</li>
 * </ul>
 *
 * <h2>tickCheck</h2>
 * <p>Chunks that fail the check are left in
 * {@link #nextTickForContainer} for a future tick; nothing is polled from their
 * container. This preserves {@code hasScheduledTick} correctness and avoids
 * spurious callbacks.
 */
@Log4j2
public class BlockUpdateScheduler {

    private static final Hash.Strategy<BlockUpdateEntry> ENTRY_UNIQUE_HASH =
            new Hash.Strategy<>() {
                @Override
                public int hashCode(BlockUpdateEntry e) {
                    return e == null ? 0 : e.hashCode();
                }

                @Override
                public boolean equals(BlockUpdateEntry a, BlockUpdateEntry b) {
                    if (a == b) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };

    private final CloudLevel level;

    /**
     * Gates tick execution by chunk key. Applied during
     * {@link #sortContainersToTick}: only chunks whose key passes this test
     * have their containers moved into the drain queue.
     */
    private final LongPredicate tickCheck;

    /**
     * Maps packed chunk key to the per-chunk tick container.
     * Containers survive across unload/reload cycles (dormant while
     * unregistered). Permanently removed only via {@link #removeTickContainer}.
     */
    private final Long2ObjectOpenHashMap<LevelChunkTicks> chunkTicks;

    /**
     * Mirrors the next-due tick time per registered chunk key.
     * Only registered chunks with pending ticks appear here.
     * Updated via the {@code onTickAdded} callback.
     */
    private final Long2LongOpenHashMap nextTickForContainer;

    /**
     * Provides fine-grained read/write separation.
     * Read-only queries use optimistic or shared stamps.
     * All mutations use exclusive write stamps.
     */
    private final StampedLock lock = new StampedLock();

    /**
     * Containers with due ticks this tick, ordered by INTRA_TICK_ORDER on head.
     * The comparator is null-safe so an unexpectedly empty container does not
     * throw.
     */
    private final PriorityQueue<LevelChunkTicks> containersToTick;

    /**
     * Entries collected this tick, in execution order.
     */
    private final ArrayDeque<BlockUpdateEntry> toRunThisTick = new ArrayDeque<>();

    /**
     * Entries that have already executed this tick.
     */
    private final List<BlockUpdateEntry> alreadyRunThisTick = new ArrayList<>();

    /**
     * Reusable set for {@link #willTickThisTick} lookups. Populated lazily
     * via {@link #buildTickSetIfNeeded} and cleared by {@link #cleanupAfterTick}
     * without reallocation.
     */
    private final ObjectOpenCustomHashSet<BlockUpdateEntry> toRunThisTickSet;

    /**
     * Reusable scratch list: chunk keys to remove from
     * {@link #nextTickForContainer} during {@link #sortContainersToTick}.
     * Cleared after each use; never reallocated.
     */
    private final LongArrayList sortScratchRemove = new LongArrayList();

    /**
     * Reusable scratch list: chunk keys whose stored next-tick value needs
     * updating during {@link #sortContainersToTick}.
     * Cleared after each use; never reallocated.
     */
    private final LongArrayList sortScratchUpdateKeys = new LongArrayList();

    /**
     * Reusable scratch list: replacement next-tick values paired with
     * {@link #sortScratchUpdateKeys}.
     * Cleared after each use; never reallocated.
     */
    private final LongArrayList sortScratchUpdateVals = new LongArrayList();

    private long lastTick;

    public BlockUpdateScheduler(CloudLevel level, long currentTick, LongPredicate tickCheck) {
        this.level = level;
        this.lastTick = currentTick;
        this.tickCheck = tickCheck;
        this.chunkTicks = new Long2ObjectOpenHashMap<>();
        this.nextTickForContainer = new Long2LongOpenHashMap();
        this.nextTickForContainer.defaultReturnValue(Long.MAX_VALUE);
        this.containersToTick = new PriorityQueue<>((a, b) -> {
            BlockUpdateEntry ha = a.peek();
            BlockUpdateEntry hb = b.peek();
            if (ha == null && hb == null) return 0;
            if (ha == null) return 1;
            if (hb == null) return -1;
            return BlockUpdateEntry.INTRA_TICK_ORDER.compare(ha, hb);
        });
        this.toRunThisTickSet = new ObjectOpenCustomHashSet<>(ENTRY_UNIQUE_HASH);
    }

    /**
     * Registers (or re-registers after dormancy) the tick container for
     * {@code chunkKey}. Called when a chunk becomes fully loaded.
     */
    public void registerTickContainer(long chunkKey) {
        long stamp = lock.writeLock();
        try {
            LevelChunkTicks container = chunkTicks.computeIfAbsent(chunkKey, k -> new LevelChunkTicks());
            wireCallback(chunkKey, container);
            long next = container.getNextTickTime();
            if (next != Long.MAX_VALUE) {
                nextTickForContainer.put(chunkKey, next);
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Makes the container dormant without discarding its data.
     * Called when a chunk is unloaded. Pending ticks survive the cycle.
     */
    public void unregisterTickContainer(long chunkKey) {
        long stamp = lock.writeLock();
        try {
            LevelChunkTicks container = chunkTicks.get(chunkKey);
            if (container == null) return;
            container.setOnTickAdded(null);
            nextTickForContainer.remove(chunkKey);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Permanently discards the container for {@code chunkKey}.
     * Called only after the chunk's tick data has been written to disk.
     *
     * <p>If the chunk was reloaded before this call (i.e., the container was
     * re-registered), the container now has a live callback and must not be
     * removed. The guard is the presence of the callback: a dormant/pending-save
     * container always has a null callback; a re-registered container does not.
     */
    public void removeTickContainer(long chunkKey) {
        long stamp = lock.writeLock();
        try {
            LevelChunkTicks container = chunkTicks.get(chunkKey);
            if (container == null) return;
            if (container.getOnTickAdded() != null) return;
            chunkTicks.remove(chunkKey);
            nextTickForContainer.remove(chunkKey);
            container.setOnTickAdded(null);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void tick(long currentTick, int maxTicks) {
        long stamp = lock.writeLock();
        try {
            if (currentTick - lastTick < Short.MAX_VALUE) {
                for (long t = lastTick + 1; t <= currentTick; t++) {
                    stamp = perform(t, maxTicks, stamp);
                }
            } else {
                stamp = perform(currentTick, maxTicks, stamp);
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Executes one game-tick worth of block ticks.
     *
     * <p>The method is split into three lock-separated phases to avoid a
     * non-reentrant deadlock:
     * <ol>
     *   <li><b>Collection phase</b> (write lock held): drain due containers
     *       into {@link #toRunThisTick}.</li>
     *   <li><b>Run phase</b> (lock released): execute each collected entry.
     *       Block tick callbacks may call {@link #add}, which acquires the
     *       write lock independently. This is safe because the run phase does
     *       not touch the shared scheduler state.</li>
     *   <li><b>Cleanup phase</b> (write lock re-acquired): clear scratch
     *       structures.</li>
     * </ol>
     *
     * @param currentTick the tick being processed
     * @param maxTicks    maximum entries to collect this tick
     * @param stamp       the write stamp held by the caller; may be exchanged
     *                    and returned as a fresh stamp after re-acquisition
     * @return the current write stamp (caller must unlock it)
     */
    private long perform(long currentTick, int maxTicks, long stamp) {
        lastTick = currentTick;
        if (nextTickForContainer.isEmpty()) {
            return stamp;
        }

        // Phase 1: collection (write lock held by caller).
        sortContainersToTick(currentTick);
        drainContainers(currentTick, maxTicks);
        rescheduleLeftoverContainers();

        // Phase 2: execution (lock released so callbacks can call add()).
        lock.unlockWrite(stamp);
        try {
            runCollectedTicks();
        } finally {
            // Phase 3: cleanup (re-acquire write lock).
            stamp = lock.writeLock();
        }

        cleanupAfterTick();
        return stamp;
    }

    /**
     * Moves containers whose head tick is due and whose chunk passes
     * {@link #tickCheck} into {@link #containersToTick}.
     *
     * <p>Iterates {@link #nextTickForContainer} key-by-key (non-fast iterator)
     * and collects mutations into side lists, applying them after the loop.
     * Mutating a fastutil open-addressing map via a fast iterator's
     * {@code remove()} can corrupt the iterator's internal slot index due to
     * Robin Hood backward-shifting.
     */
    private void sortContainersToTick(long currentTick) {
        for (long key : nextTickForContainer.keySet().toLongArray()) {
            long storedNext = nextTickForContainer.get(key);

            if (storedNext > currentTick) {
                continue;
            }

            LevelChunkTicks container = chunkTicks.get(key);
            if (container == null) {
                sortScratchRemove.add(key);
                continue;
            }

            BlockUpdateEntry head = container.peek();
            if (head == null) {
                sortScratchRemove.add(key);
                continue;
            }

            if (head.delay > currentTick) {
                sortScratchUpdateKeys.add(key);
                sortScratchUpdateVals.add(head.delay);
                continue;
            }

            if (!tickCheck.test(key)) {
                continue;
            }

            sortScratchRemove.add(key);
            containersToTick.add(container);
        }

        for (int i = 0; i < sortScratchRemove.size(); i++) {
            nextTickForContainer.remove(sortScratchRemove.getLong(i));
        }

        for (int i = 0; i < sortScratchUpdateKeys.size(); i++) {
            nextTickForContainer.put(sortScratchUpdateKeys.getLong(i), sortScratchUpdateVals.getLong(i));
        }

        sortScratchRemove.clear();
        sortScratchUpdateKeys.clear();
        sortScratchUpdateVals.clear();
    }

    /**
     * Drains up to {@code maxTicks} entries from {@link #containersToTick}
     * into {@link #toRunThisTick}, interleaving across containers in global
     * {@link BlockUpdateEntry#INTRA_TICK_ORDER}.
     */
    private void drainContainers(long currentTick, int maxTicks) {
        LevelChunkTicks container;
        while (canCollectMore(maxTicks) && (container = containersToTick.poll()) != null) {
            BlockUpdateEntry first = container.poll();
            scheduleForThisTick(first);

            drainFromCurrentContainer(container, currentTick, maxTicks);

            BlockUpdateEntry nextHead = container.peek();
            if (nextHead != null) {
                if (nextHead.delay <= currentTick && canCollectMore(maxTicks)) {
                    containersToTick.add(container);
                } else {
                    updateContainerScheduling(nextHead);
                }
            }
        }
    }

    /**
     * Continues draining {@code container} as long as its head has higher or
     * equal priority versus the next container in {@link #containersToTick},
     * and entries are still due this tick.
     */
    private void drainFromCurrentContainer(LevelChunkTicks container, long currentTick, int maxTicks) {
        if (!canCollectMore(maxTicks)) {
            return;
        }

        LevelChunkTicks nextContainer = containersToTick.peek();
        BlockUpdateEntry nextContainerHead = (nextContainer != null) ? nextContainer.peek() : null;

        while (canCollectMore(maxTicks)) {
            BlockUpdateEntry head = container.peek();
            if (head == null || head.delay > currentTick) {
                break;
            }

            if (nextContainerHead != null && BlockUpdateEntry.INTRA_TICK_ORDER.compare(head, nextContainerHead) > 0) {
                break;
            }

            container.poll();
            scheduleForThisTick(head);
        }
    }

    private void scheduleForThisTick(BlockUpdateEntry entry) {
        toRunThisTick.addLast(entry);
    }

    private boolean canCollectMore(int maxTicks) {
        return toRunThisTick.size() < maxTicks;
    }

    /**
     * Any container still in {@link #containersToTick} after the drain cap was
     * hit has its head time written back to {@link #nextTickForContainer}.
     */
    private void rescheduleLeftoverContainers() {
        for (LevelChunkTicks container : containersToTick) {
            BlockUpdateEntry head = container.peek();
            if (head != null) {
                updateContainerScheduling(head);
            }
        }
    }

    private void updateContainerScheduling(BlockUpdateEntry head) {
        nextTickForContainer.put(chunkKey(head.pos), head.delay);
    }

    private void runCollectedTicks() {
        while (!toRunThisTick.isEmpty()) {
            BlockUpdateEntry entry = toRunThisTick.pollFirst();

            if (!toRunThisTickSet.isEmpty()) {
                toRunThisTickSet.remove(entry);
            }

            alreadyRunThisTick.add(entry);

            Vector3i pos = entry.pos;
            Block block = level.getBlock(pos);

            if (entry.block.getState().getType() == block.getState().getType()) {
                TickBlockHandler onTick = block.getComponents().get(BlockComponents.ON_TICK);
                if (onTick != null) {
                    onTick.execute(block, null);
                }
            }

            BlockState extraState = block.getExtra();
            if (entry.block.getExtra().getType() == extraState.getType() && extraState != BlockStates.AIR) {
                ComponentMap extraComponents = CloudBlockRegistry.REGISTRY.getComponents(extraState.getType());
                TickBlockHandler extraOnTick = extraComponents.get(BlockComponents.ON_TICK);
                if (extraOnTick != null) {
                    extraOnTick.execute(block, null);
                }
            }
        }
    }

    private void cleanupAfterTick() {
        toRunThisTick.clear();
        containersToTick.clear();
        alreadyRunThisTick.clear();
        toRunThisTickSet.clear();
        sortScratchRemove.clear();
        sortScratchUpdateKeys.clear();
        sortScratchUpdateVals.clear();
    }

    public void add(BlockUpdateEntry entry) {
        long stamp = lock.writeLock();
        try {
            addUnderLock(entry);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private void addUnderLock(BlockUpdateEntry entry) {
        long key = chunkKey(entry.pos);
        LevelChunkTicks container = chunkTicks.get(key);
        if (container == null) {
            log.warn("Tried to schedule block tick at {} in unloaded chunk (chunk key {}); dropping entry.", entry.pos, key);
            return;
        }

        long minTime = lastTick + 1;
        BlockUpdateEntry toSchedule = (entry.delay >= minTime) ? entry : BlockUpdateEntry.ofWithId(entry.pos, entry.block, minTime, entry.id);
        container.schedule(toSchedule);
    }

    /**
     * Returns all pending entries for the given chunk key as a snapshot list,
     * or {@code null} if no container exists for that key.
     *
     * <p>The list is fully materialized while a shared read lock is held, so
     * it is safe to iterate after the method returns. Intended for use by the
     * persistence layer during chunk save.
     */
    public List<BlockUpdateEntry> packAll(long chunkKey) {
        long stamp = lock.readLock();
        try {
            LevelChunkTicks container = chunkTicks.get(chunkKey);
            if (container == null) return null;
            return container.packAll();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Records a successful save of tick data for the given chunk.
     *
     * <p>Clears the dirty flag and updates the {@code lastSaved} timestamp so
     * that {@link LevelChunkTicks#isDirty(long)} returns {@code false} until
     * the next structural mutation or game-tick advancement.
     *
     * @param chunkKey packed chunk key
     * @param tick     the game tick at which the save occurred
     */
    public void markSaved(long chunkKey, long tick) {
        long stamp = lock.writeLock();
        try {
            LevelChunkTicks container = chunkTicks.get(chunkKey);
            if (container != null) {
                container.markSaved(tick);
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns {@code true} when the tick data for the given chunk needs to be
     * written to disk: either because entries were added or removed since the
     * last save, or because game time has advanced and the relative-delay
     * encoding in the stored record is now stale.
     *
     * @param chunkKey    packed chunk key
     * @param currentTick the current world game tick
     */
    public boolean isDirty(long chunkKey, long currentTick) {
        long stamp = lock.tryOptimisticRead();
        LevelChunkTicks container = chunkTicks.get(chunkKey);
        boolean result = container != null && container.isDirty(currentTick);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                container = chunkTicks.get(chunkKey);
                result = container != null && container.isDirty(currentTick);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    /**
     * O(1) check via the per-chunk dedup index.
     */
    public boolean contains(BlockUpdateEntry entry) {
        long stamp = lock.tryOptimisticRead();
        LevelChunkTicks container = chunkTicks.get(chunkKey(entry.pos));
        boolean result = container != null && container.contains(entry);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                container = chunkTicks.get(chunkKey(entry.pos));
                result = container != null && container.contains(entry);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if the given {@code (pos, block)} tick has been
     * collected this game tick but has not yet executed.
     *
     * <p>The backing set is built lazily on the first call per tick and cleared
     * by {@link #cleanupAfterTick} (without reallocation).
     *
     * <p>Acquires a write lock because {@link #buildTickSetIfNeeded} may
     * populate {@link #toRunThisTickSet}, which is a mutation. This is safe
     * to call from block tick callbacks (during the run phase) because the run
     * phase releases the write lock before executing ticks.
     */
    public boolean willTickThisTick(Vector3i pos, Block block) {
        long stamp = lock.writeLock();
        try {
            if (toRunThisTick.isEmpty()) return false;
            buildTickSetIfNeeded();
            return toRunThisTickSet.contains(BlockUpdateEntry.probe(pos, block));
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public Set<BlockUpdateEntry> getPendingBlockUpdates(AxisAlignedBB bb) {
        long stamp = lock.readLock();
        try {
            Set<BlockUpdateEntry> result = null;
            for (long key : chunkKeysInBounds(bb)) {
                LevelChunkTicks container = chunkTicks.get(key);
                if (container == null) continue;
                Set<BlockUpdateEntry> partial = container.getPendingInBounds(bb);
                if (partial != null) {
                    if (result == null) result = new java.util.HashSet<>();
                    result.addAll(partial);
                }
            }
            return result;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * O(1) removal via the per-chunk dedup index.
     */
    public boolean remove(BlockUpdateEntry entry) {
        long stamp = lock.writeLock();
        try {
            long key = chunkKey(entry.pos);
            LevelChunkTicks container = chunkTicks.get(key);
            if (container == null) return false;
            boolean removed = container.remove(entry);
            if (removed) {
                if (container.isEmpty()) {
                    nextTickForContainer.remove(key);
                } else {
                    nextTickForContainer.put(key, container.getNextTickTime());
                }
            }
            return removed;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Removes all pending ticks within {@code bb} from all containers and from
     * the current tick's collected / already-run lists.
     */
    public void clearArea(AxisAlignedBB bb) {
        long stamp = lock.writeLock();
        try {
            Predicate<BlockUpdateEntry> inBounds = e -> isInBounds(e, bb);

            for (long key : chunkKeysInBounds(bb)) {
                LevelChunkTicks container = chunkTicks.get(key);
                if (container == null) {
                    continue;
                }

                BlockUpdateEntry prevHead = container.peek();
                int removed = container.clearInBounds(bb);
                if (removed > 0) {
                    BlockUpdateEntry newHead = container.peek();
                    if (newHead != prevHead) {
                        if (newHead != null) {
                            nextTickForContainer.put(key, newHead.delay);
                        } else {
                            nextTickForContainer.remove(key);
                        }
                    }
                }
            }

            toRunThisTick.removeIf(inBounds);
            alreadyRunThisTick.removeIf(inBounds);
            toRunThisTickSet.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Copies all pending ticks within {@code bb} from this scheduler to the
     * offset position in this scheduler.
     *
     * <p>Equivalent to {@code copyAreaFrom(this, bb, offset)}.
     *
     * @param bb     source bounding box
     * @param offset block position offset applied to each copied tick
     */
    public void copyArea(AxisAlignedBB bb, Vector3i offset) {
        copyAreaFrom(this, bb, offset);
    }

    /**
     * Copies all pending ticks within {@code bb} from {@code source} into
     * this scheduler at the offset position.
     *
     * <p>When {@code source} is this scheduler, a single write lock is held
     * throughout. When {@code source} is a different scheduler, the source
     * entries are snapshotted under the source read lock first, then the
     * destination write lock is acquired separately to avoid deadlock.
     *
     * <p>Also-run and collected-but-not-run entries from the <em>destination</em>
     * tick are used to compute the global max ID, matching the semantics of
     * the self-copy path: copies always sort after every entry that has touched
     * this scheduler this tick.
     *
     * @param source the scheduler to read ticks from
     * @param bb     source bounding box
     * @param offset block position offset applied to each copied tick
     */
    public void copyAreaFrom(BlockUpdateScheduler source, AxisAlignedBB bb, Vector3i offset) {
        if (source == this) {
            long stamp = lock.writeLock();
            try {
                copyAreaUnderWriteLock(source, bb, offset);
            } finally {
                lock.unlockWrite(stamp);
            }
            return;
        }

        Predicate<BlockUpdateEntry> inBounds = e -> isInBounds(e, bb);
        List<BlockUpdateEntry> sources;

        long srcStamp = source.lock.readLock();
        try {
            sources = new ArrayList<>();
            source.alreadyRunThisTick.stream().filter(inBounds).forEach(sources::add);
            source.toRunThisTick.stream().filter(inBounds).forEach(sources::add);
            for (long key : chunkKeysInBounds(bb)) {
                LevelChunkTicks container = source.chunkTicks.get(key);
                if (container != null) {
                    container.getAll().filter(inBounds).forEach(sources::add);
                }
            }
        } finally {
            source.lock.unlockRead(srcStamp);
        }

        if (sources.isEmpty()) {
            return;
        }

        long stamp = lock.writeLock();
        try {
            insertCopiedEntries(sources, offset);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Implements the copy logic when the write lock on {@code this} is already
     * held. When {@code source == this}, reads from live scratch structures
     * directly. Extracted so the same logic is shared by both lock paths.
     */
    private void copyAreaUnderWriteLock(BlockUpdateScheduler source, AxisAlignedBB bb, Vector3i offset) {
        Predicate<BlockUpdateEntry> inBounds = e -> isInBounds(e, bb);
        List<BlockUpdateEntry> sources = new ArrayList<>();

        source.alreadyRunThisTick.stream().filter(inBounds).forEach(sources::add);
        source.toRunThisTick.stream().filter(inBounds).forEach(sources::add);

        for (long key : chunkKeysInBounds(bb)) {
            LevelChunkTicks container = source.chunkTicks.get(key);
            if (container != null) {
                container.getAll().filter(inBounds).forEach(sources::add);
            }
        }

        if (sources.isEmpty()) {
            return;
        }
        insertCopiedEntries(sources, offset);
    }

    /**
     * Rebases and inserts a pre-collected list of source entries into this
     * scheduler. Must be called while holding the write lock on {@code this}.
     *
     * <p>The ID range of the copies is shifted so they sort after every entry
     * that has already touched this scheduler this tick.
     */
    private void insertCopiedEntries(List<BlockUpdateEntry> sources, Vector3i offset) {
        long globalIdMax = toRunThisTick.stream()
                .mapToLong(e -> e.id)
                .max()
                .orElse(-1L);
        long alreadyMax = alreadyRunThisTick.stream()
                .mapToLong(e -> e.id)
                .max()
                .orElse(-1L);
        globalIdMax = Math.max(globalIdMax, alreadyMax);

        long idMin = sources.stream()
                .mapToLong(e -> e.id)
                .min()
                .orElse(0L);

        for (BlockUpdateEntry src : sources) {
            Vector3i newPos = src.pos.add(offset);
            long newDelay = Math.max(src.delay, lastTick + 1);
            long newId = (src.id - idMin) + globalIdMax + 1;
            BlockUpdateEntry copy = BlockUpdateEntry.ofWithId(newPos, src.block, newDelay, newId);
            addUnderLock(copy);
        }
    }

    /**
     * Wires the {@code onTickAdded} callback so {@link #nextTickForContainer}
     * is updated only when a new entry becomes the queue head of a registered
     * container. The head-check is done here in the scheduler callback (not
     * inside the container).
     */
    private void wireCallback(long key, LevelChunkTicks container) {
        container.setOnTickAdded((c, newEntry) -> {
            BlockUpdateEntry head = c.peek();
            if (head == newEntry) {
                nextTickForContainer.put(key, newEntry.delay);
            }
        });
    }

    /**
     * Populates {@link #toRunThisTickSet} from {@link #toRunThisTick} if the
     * set is currently empty and the deque is not. The set is reused across
     * calls within the same tick cycle without reallocation.
     */
    private void buildTickSetIfNeeded() {
        if (toRunThisTickSet.isEmpty() && !toRunThisTick.isEmpty()) {
            toRunThisTickSet.addAll(toRunThisTick);
        }
    }

    private static long chunkKey(Vector3i pos) {
        return CloudChunk.key(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /**
     * Returns all packed chunk keys that overlap the XZ footprint of {@code bb}.
     */
    private static long[] chunkKeysInBounds(AxisAlignedBB bb) {
        int minChunkX = (int) Math.floor(bb.getMinX()) >> 4;
        int minChunkZ = (int) Math.floor(bb.getMinZ()) >> 4;
        int maxChunkX = (int) Math.floor(bb.getMaxX()) >> 4;
        int maxChunkZ = (int) Math.floor(bb.getMaxZ()) >> 4;

        int count = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        long[] keys = new long[count];
        int i = 0;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                keys[i++] = CloudChunk.key(cx, cz);
            }
        }
        return keys;
    }

    private static boolean isInBounds(BlockUpdateEntry entry, AxisAlignedBB bb) {
        Vector3i p = entry.pos;
        return p.getX() >= bb.getMinX() && p.getX() < bb.getMaxX()
                && p.getY() >= bb.getMinY() && p.getY() < bb.getMaxY()
                && p.getZ() >= bb.getMinZ() && p.getZ() < bb.getMaxZ();
    }
}
