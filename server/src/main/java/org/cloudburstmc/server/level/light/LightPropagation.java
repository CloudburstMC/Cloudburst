package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;

/**
 * Spreads one channel's light out from edited positions.
 *
 * <p>Two queues hold the work: decreasing entries remember the light a cell
 * used to spread, increasing entries spread what cells carry now. Decreases
 * always drain first so teardown cannot chase fresh light.
 *
 * <p>{@link #run} processes at most {@code maxSteps} entries in total and
 * returns true once both queues are empty.
 */
final class LightPropagation {

    private final LightChannel channel;
    private final LongArrayFIFOQueue decreasePositions = new LongArrayFIFOQueue();
    /**
     * Old levels parallel to decreasePositions; a second long queue
     * because the dependency bundle has no int FIFO.
     */
    private final LongArrayFIFOQueue decreaseLevels = new LongArrayFIFOQueue();
    private final LongArrayFIFOQueue increasePositions = new LongArrayFIFOQueue();

    LightPropagation(LightChannel channel) {
        this.channel = channel;
    }

    /**
     * Re-queues a changed position and its neighbourhood.
     *
     * <p>Direct neighbours are first relaxed to {@code recompute}, because the
     * precise decrease check below computes arrivals with post-change states
     * and cannot see support that vanished with the edit itself: when a slab
     * seals the shared face, the arrival from the edited cell drops to 0 while
     * the neighbour still shows its old light, which then looks self-supported.
     * Relaxing catches exactly this one-cell weakening; wider cascades are left
     * to the decrease pass, keeping teardown bounded.
     */
    void invalidate(LightAccess access, long pos) {
        int x = LightPositions.x(pos);
        int y = LightPositions.y(pos);
        int z = LightPositions.z(pos);
        int old = access.light(x, y, z);
        BlockState primary = access.primary(x, y, z);
        BlockState secondary = access.secondary(x, y, z);
        int own = this.channel.ownValue(access.states(), primary, secondary, y, access.maxHeight());
        if (own != old) {
            access.setLight(x, y, z, own);
        }
        if (old != own) {
            this.decreasePositions.enqueue(pos);
            this.decreaseLevels.enqueue(old);
        }
        int minHeight = access.minHeight();
        int maxHeight = access.maxHeight();
        for (int i = 0; i < LightChannel.DIRECTIONS.length; i++) {
            Direction direction = LightChannel.DIRECTIONS[i];
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();
            if (ny < minHeight || ny >= maxHeight || !access.isLoaded(nx, nz)) {
                continue;
            }
            int current = access.light(nx, ny, nz);
            int relaxed = this.channel.recompute(access, nx, ny, nz);
            if (relaxed < current) {
                access.setLight(nx, ny, nz, relaxed);
                this.decreasePositions.enqueue(LightPositions.pack(nx, ny, nz));
                this.decreaseLevels.enqueue(current);
            }
            this.increasePositions.enqueue(LightPositions.pack(nx, ny, nz));
        }
        if (own > 0) {
            this.increasePositions.enqueue(pos);
        }
    }

    void source(long pos) {
        this.increasePositions.enqueue(pos);
    }

    /**
     * Runs at most {@code remaining[0]} entries in total, decrements the budget,
     * and returns true once both queues are empty.
     *
     * @param access    the light access
     * @param remaining the shared step budget, decremented by the steps taken
     * @return true once both queues are empty
     */
    boolean run(LightAccess access, int[] remaining) {
        int steps = 0;
        while (steps < remaining[0] && (!this.decreasePositions.isEmpty() || !this.increasePositions.isEmpty())) {
            if (!this.decreasePositions.isEmpty()) {
                long position = this.decreasePositions.dequeueLong();
                int oldLevel = (int) this.decreaseLevels.dequeueLong();
                decrease(access, position, oldLevel);
            } else {
                increase(access, this.increasePositions.dequeueLong());
            }
            steps++;
        }
        remaining[0] -= steps;
        return this.decreasePositions.isEmpty() && this.increasePositions.isEmpty();
    }

    private void decrease(LightAccess access, long position, int oldLevel) {
        int x = LightPositions.x(position);
        int y = LightPositions.y(position);
        int z = LightPositions.z(position);
        BlockState fromPrimary = access.primary(x, y, z);
        BlockState fromSecondary = access.secondary(x, y, z);
        LightStateCache states = access.states();
        int minHeight = access.minHeight();
        int maxHeight = access.maxHeight();
        for (int i = 0; i < LightChannel.DIRECTIONS.length; i++) {
            Direction direction = LightChannel.DIRECTIONS[i];
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();
            if (ny < minHeight || ny >= maxHeight || !access.isLoaded(nx, nz)) {
                continue;
            }
            int current = access.light(nx, ny, nz);
            if (current <= 0) {
                continue;
            }
            BlockState toPrimary = access.primary(nx, ny, nz);
            BlockState toSecondary = access.secondary(nx, ny, nz);
            // Arrivals never exceed the spread level, so a neighbour already
            // above it keeps its light and only needs a fresh increase visit.
            if (current <= oldLevel && current <= this.channel.arrive(states, fromPrimary, fromSecondary,
                    toPrimary, toSecondary, direction, oldLevel)) {
                int own = this.channel.ownValue(states, toPrimary, toSecondary, ny, maxHeight);
                // Only a changed cell withdraws support downstream; an unchanged
                // cell still carries the same light, and re-queueing it lets
                // neighbouring emitters re-queue each other without bound.
                if (own != current) {
                    access.setLight(nx, ny, nz, own);
                    this.decreasePositions.enqueue(LightPositions.pack(nx, ny, nz));
                    this.decreaseLevels.enqueue(current);
                }
                if (own > 0) {
                    this.increasePositions.enqueue(LightPositions.pack(nx, ny, nz));
                }
            } else {
                this.increasePositions.enqueue(LightPositions.pack(nx, ny, nz));
            }
        }
    }

    private void increase(LightAccess access, long position) {
        int x = LightPositions.x(position);
        int y = LightPositions.y(position);
        int z = LightPositions.z(position);
        if (y < access.minHeight() || y >= access.maxHeight() || !access.isLoaded(x, z)) {
            return;
        }
        int level = access.light(x, y, z);
        if (level <= 0) {
            return;
        }
        BlockState fromPrimary = access.primary(x, y, z);
        BlockState fromSecondary = access.secondary(x, y, z);
        LightStateCache states = access.states();
        int minHeight = access.minHeight();
        int maxHeight = access.maxHeight();
        for (int i = 0; i < LightChannel.DIRECTIONS.length; i++) {
            Direction direction = LightChannel.DIRECTIONS[i];
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();
            if (ny < minHeight || ny >= maxHeight || !access.isLoaded(nx, nz)) {
                continue;
            }
            // Arrivals never exceed the source level, so a neighbour already
            // at or above it cannot rise from here.
            int current = access.light(nx, ny, nz);
            if (current >= level) {
                continue;
            }
            int arrival = this.channel.arrive(states, fromPrimary, fromSecondary,
                    access.primary(nx, ny, nz), access.secondary(nx, ny, nz), direction, level);
            if (arrival > current) {
                access.setLight(nx, ny, nz, arrival);
                this.increasePositions.enqueue(LightPositions.pack(nx, ny, nz));
            }
        }
    }
}
