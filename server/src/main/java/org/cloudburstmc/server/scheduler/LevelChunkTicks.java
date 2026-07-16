package org.cloudburstmc.server.scheduler;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.utils.BlockUpdateEntry;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Per-chunk container for scheduled block ticks.
 *
 * <p>Entries are held in a {@link PriorityQueue} ordered by
 * {@link BlockUpdateEntry#DRAIN_ORDER} (delay, id). A parallel
 * {@link ObjectOpenCustomHashSet} provides O(1) membership and deduplication
 * keyed on position and scheduled type.
 *
 * <p>{@link #nextTickTime} tracks the head entry's {@code delay} so the
 * level scheduler can skip this container without touching the queue.
 *
 * <p>The optional {@link #onTickAdded} callback fires after every successful
 * {@link #scheduleUnchecked}. The owning scheduler is responsible for
 * deciding whether to act on the notification (e.g. only when the entry
 * becomes the new queue head).
 *
 * <p>The scheduler controls all draining through {@link #peek()} and
 * {@link #poll()}, giving it full control over cross-container interleaving.
 *
 */
public final class LevelChunkTicks {

    private static final Hash.Strategy<BlockUpdateEntry> DEDUP_STRATEGY =
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

    /**
     * Sorted by DRAIN_ORDER: (delay, id).
     */
    private final PriorityQueue<BlockUpdateEntry> queue;

    /**
     * Deduplication index; mirrors queue contents.
     */
    private final ObjectOpenCustomHashSet<BlockUpdateEntry> index;

    /**
     * The {@code delay} of the earliest entry in the queue, or
     * {@link Long#MAX_VALUE} when empty. Updated after every structural mutation.
     */
    private long nextTickTime;

    /**
     * Optional callback invoked after every successful
     * {@link #scheduleUnchecked}. The second argument is the newly added entry.
     * The owning scheduler checks whether it became the new head.
     * Set by the owning scheduler via {@link #setOnTickAdded}.
     */
    private BiConsumer<LevelChunkTicks, BlockUpdateEntry> onTickAdded;

    /** True after entries have changed since the last successful save. */
    private boolean dirty;

    public LevelChunkTicks() {
        this.queue = new PriorityQueue<>(BlockUpdateEntry.DRAIN_ORDER);
        this.index = new ObjectOpenCustomHashSet<>(DEDUP_STRATEGY);
        this.nextTickTime = Long.MAX_VALUE;
        this.dirty = false;
    }

    public int size() {
        return this.index.size();
    }

    /**
     * Returns the current {@code onTickAdded} callback, or {@code null} if
     * none is set. Used by the scheduler to detect whether this container is
     * still dormant (null) or actively registered (non-null).
     */
    public BiConsumer<LevelChunkTicks, BlockUpdateEntry> getOnTickAdded() {
        return this.onTickAdded;
    }

    /**
     * Registers a callback invoked after each successful entry addition.
     * The second argument is the newly added entry; callers should check
     * {@link #peek()} themselves to decide if it became the new head.
     * Pass {@code null} to clear.
     */
    public void setOnTickAdded(BiConsumer<LevelChunkTicks, BlockUpdateEntry> callback) {
        this.onTickAdded = callback;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markSaved() {
        this.dirty = false;
    }

    /**
     * Adds {@code entry} if no entry with the same {@code (pos, block)} is
     * already present. Fires {@link #onTickAdded} when added.
     */
    public void schedule(BlockUpdateEntry entry) {
        if (index.add(entry)) {
            scheduleUnchecked(entry);
        }
    }

    /**
     * Adds {@code entry} without consulting the dedup index.
     * Must only be called when the caller already knows the entry is absent.
     * Always fires {@link #onTickAdded}; the scheduler callback decides
     * whether the entry became the new head.
     */
    private void scheduleUnchecked(BlockUpdateEntry entry) {
        queue.offer(entry);
        updateNextTick();
        dirty = true;
        if (onTickAdded != null) {
            onTickAdded.accept(this, entry);
        }
    }

    /**
     * Returns the head entry without removing it, or {@code null} when empty.
     */
    public BlockUpdateEntry peek() {
        return queue.peek();
    }

    /**
     * Removes and returns the head entry. Also removes it from the dedup index.
     *
     * @return the removed head entry, or {@code null} when empty
     */
    public BlockUpdateEntry poll() {
        BlockUpdateEntry entry = queue.poll();
        if (entry != null) {
            index.remove(entry);
            updateNextTick();
            dirty = true;
        }
        return entry;
    }

    /**
     * Returns {@code true} if an entry matching {@code entry} is queued (O(1)).
     */
    public boolean contains(BlockUpdateEntry entry) {
        return index.contains(entry);
    }

    /**
     * Returns {@code true} if any queued entry targets {@code pos} with block
     * {@code block}. Uses a zero-cost probe, O(1).
     */
    public boolean hasScheduledTick(Vector3i pos, BlockType type) {
        return index.contains(BlockUpdateEntry.probe(pos, type));
    }

    /**
     * Returns the delay of the earliest queued entry, or {@link Long#MAX_VALUE}.
     */
    public long getNextTickTime() {
        return nextTickTime;
    }

    /**
     * Returns {@code true} when no entries are queued.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the number of queued entries.
     */
    public int count() {
        return queue.size();
    }

    /**
     * Returns a stream over all queued entries.
     */
    public Stream<BlockUpdateEntry> getAll() {
        return queue.stream();
    }

    /**
     * Removes the entry matching {@code entry} (by dedup equality).
     *
     * @return {@code true} if an entry was removed
     */
    public boolean remove(BlockUpdateEntry entry) {
        if (!index.remove(entry)) {
            return false;
        }
        queue.remove(entry);
        updateNextTick();
        dirty = true;
        return true;
    }

    /**
     * Removes all entries matching {@code predicate}.
     *
     * <p>Iterates the queue directly so each removal is O(1) via the iterator,
     * giving O(n) total. The dedup index is updated in the same pass.
     *
     * @return number of entries removed
     */
    public int removeIf(Predicate<BlockUpdateEntry> predicate) {
        int removed = 0;
        Iterator<BlockUpdateEntry> it = queue.iterator();
        while (it.hasNext()) {
            BlockUpdateEntry entry = it.next();
            if (predicate.test(entry)) {
                it.remove();
                index.remove(entry);
                removed++;
            }
        }
        if (removed > 0) {
            updateNextTick();
            dirty = true;
        }
        return removed;
    }

    /**
     * Returns all queued entries whose positions fall within the 3-D bounds of
     * {@code bb}. Returns {@code null} when none match.
     */
    public Set<BlockUpdateEntry> getPendingInBounds(BoundingBox bb) {
        Set<BlockUpdateEntry> result = null;
        for (BlockUpdateEntry entry : index) {
            if (isInBounds(entry, bb)) {
                if (result == null) {
                    result = new HashSet<>();
                }
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Removes all queued entries whose positions fall within the 3-D bounds of
     * {@code bb}.
     *
     * @return number of entries removed
     */
    public int clearInBounds(BoundingBox bb) {
        return removeIf(e -> isInBounds(e, bb));
    }

    /**
     * Returns all queued entries as a materialized list for serialization.
     * Materializes eagerly so the caller does not need to hold any lock to
     * iterate the result.
     */
    public List<BlockUpdateEntry> packAll() {
        return new ArrayList<>(queue);
    }

    private void updateNextTick() {
        BlockUpdateEntry head = queue.peek();
        nextTickTime = (head != null) ? head.delay : Long.MAX_VALUE;
    }

    private static boolean isInBounds(BlockUpdateEntry entry, BoundingBox bb) {
        Vector3i p = entry.pos;
        return p.getX() >= bb.getMinX() && p.getX() < bb.getMaxX()
                && p.getY() >= bb.getMinY() && p.getY() < bb.getMaxY()
                && p.getZ() >= bb.getMinZ() && p.getZ() < bb.getMaxZ();
    }
}
