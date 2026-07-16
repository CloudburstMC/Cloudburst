package org.cloudburstmc.server.utils;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;


/**
 * A single scheduled block tick entry.
 *
 * <h2>Ordering</h2>
 * <p>Ordering is handled through explicit comparators rather than {@link Comparable}.
 * <ul>
 *   <li>{@link #DRAIN_ORDER}: sorts by {@code (delay, id)}. Used by per-chunk priority
 *       queues so entries fire in correct absolute tick order.</li>
 *   <li>{@link #INTRA_TICK_ORDER}: sorts by {@code id} only. Used when interleaving
 *       entries across containers within a single tick, where all entries are already due.</li>
 * </ul>
 *
 * <h2>Deduplication</h2>
 * <p>Equality and hashing are keyed on position and the scheduled block type. Primary and
 * secondary layer states can therefore retain independent ticks at the same position.
 *
 * <h2>Factories</h2>
 * <ul>
 *   <li>{@link #of}: creates a new tick with a unique positive ID.</li>
 *   <li>{@link #ofRestored}: creates a tick restored from disk. Uses a counter starting
 *       at {@link Long#MIN_VALUE} so restored ticks always sort before live ticks within
 *       the same game tick.</li>
 *   <li>{@link #ofWithId}: creates a tick with an explicit ID, used by area-copy operations.</li>
 *   <li>{@link #probe}: creates a zero-cost sentinel for set-membership checks only.</li>
 * </ul>
 */
public final class BlockUpdateEntry {

    /**
     * Full ordering: {@code delay → id}.
     * Used for the per-chunk {@link java.util.PriorityQueue}.
     */
    public static final Comparator<BlockUpdateEntry> DRAIN_ORDER =
            Comparator.comparingLong((BlockUpdateEntry e) -> e.delay)
                    .thenComparingLong(e -> e.id);

    /**
     * Intra-tick ordering: {@code id} only (no delay).
     * Used when interleaving ticks across containers within a single game tick,
     * where all entries are already gated to be due.
     */
    public static final Comparator<BlockUpdateEntry> INTRA_TICK_ORDER =
            Comparator.comparingLong((BlockUpdateEntry e) -> e.id);

    /**
     * Monotonically increasing insertion counter for newly-scheduled ticks.
     * Starts at {@code 0} and increments upward.
     * AtomicLong ensures thread safety when entries are created outside the
     * scheduler's own lock (e.g., from block tick callbacks or plugins).
     */
    private static final AtomicLong nextId = new AtomicLong(0);

    /**
     * Monotonically increasing insertion counter for ticks restored from disk.
     * Starts at {@link Long#MIN_VALUE} and increments upward toward {@code -1}.
     *
     * <p>Because all restored IDs are negative and all live IDs are
     * non-negative, {@link #DRAIN_ORDER} (and {@link #INTRA_TICK_ORDER}) will
     * always sort restored ticks before newly-scheduled ticks when both are
     * due in the same game tick. This matches the intended sub-tick ordering:
     * a block that was pending before a save/load cycle fires before any tick
     * that was scheduled after the load completed.
     */
    private static final AtomicLong restoredId = new AtomicLong(Long.MIN_VALUE);

    /**
     * Absolute target game tick.
     */
    public final long delay;

    public final Vector3i pos;
    public final BlockType type;

    /**
     * Sub-tick insertion counter used as a tie-breaker in ordering.
     * <ul>
     *   <li>Probe entries: {@code 0}.</li>
     *   <li>Restored entries: negative (from {@link #restoredId}).</li>
     *   <li>Live entries: non-negative (from {@link #nextId}).</li>
     * </ul>
     */
    public final long id;

    private BlockUpdateEntry(Vector3i pos, BlockType type, long delay, long id) {
        this.pos = pos;
        this.type = type;
        this.delay = delay;
        this.id = id;
    }

    /**
     * Creates a real scheduled entry. Increments the global sub-tick counter.
     *
     * @param pos   target block position
     * @param type scheduled block type
     * @param delay absolute target tick
     */
    public static BlockUpdateEntry of(Vector3i pos, BlockType type, long delay) {
        return new BlockUpdateEntry(pos, type, delay, nextId.getAndIncrement());
    }

    /**
     * Creates a lightweight probe entry for set-membership checks only.
     *
     * <p>The probe carries {@code delay = 0} and {@code id = 0}. It must never be
     * inserted into a queue or dedup set; it is only valid as an argument to
     * {@link #equals} and {@link #hashCode} lookups.
     */
    public static BlockUpdateEntry probe(Vector3i pos, BlockType type) {
        return new BlockUpdateEntry(pos, type, 0L, 0L);
    }

    /**
     * Creates a real entry for a tick that was restored from disk.
     *
     * <p>Assigns an ID from the {@link #restoredId} counter, which increments
     * from {@link Long#MIN_VALUE} upward. All restored IDs are negative and
     * therefore sort before any live ID (from {@link #of}) in
     * {@link #DRAIN_ORDER} and {@link #INTRA_TICK_ORDER}. This ensures that
     * a block pending before a save/load cycle executes before any tick that
     * was scheduled after the load completed, when both are due in the same
     * game tick.
     *
     * @param pos   target block position
     * @param type scheduled block type
     * @param delay absolute target tick
     */
    public static BlockUpdateEntry ofRestored(Vector3i pos, BlockType type, long delay) {
        return new BlockUpdateEntry(pos, type, delay, restoredId.getAndIncrement());
    }

    /**
     * Creates a real entry with an explicitly specified {@code id}.
     *
     * <p>Used by the scheduler's copy-area operation to preserve relative sub-tick
     * ordering of copied entries. The global {@link #nextId} counter is NOT advanced.
     *
     * @param pos   target block position
     * @param type scheduled block type
     * @param delay absolute target tick
     * @param id    sub-tick ordering id to use verbatim
     */
    public static BlockUpdateEntry ofWithId(Vector3i pos, BlockType type, long delay, long id) {
        return new BlockUpdateEntry(pos, type, delay, id);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BlockUpdateEntry other
                && this.pos.equals(other.pos)
                && this.type == other.type;
    }

    @Override
    public int hashCode() {
        return 31 * this.pos.hashCode() + this.type.hashCode();
    }
}
