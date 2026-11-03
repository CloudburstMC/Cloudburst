package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;

/**
 * Propagates one light channel, processing decreases before increases.
 * Instances are thread-confined and retain pending work between slices.
 */
public class LightPropagation {

    private static final Direction[] DIRECTIONS = Direction.values();

    private final LightChannel channel;
    private final LongArrayFIFOQueue decreasePositions = new LongArrayFIFOQueue();
    private final LongArrayFIFOQueue decreaseLevels = new LongArrayFIFOQueue();
    private final LongArrayFIFOQueue increasePositions = new LongArrayFIFOQueue();

    public LightPropagation(LightChannel channel) {
        this.channel = channel;
    }

    /**
     * Invalidates a changed cell and neighboring cells affected by its former light or shape.
     *
     * @param access locked access to the affected neighborhood
     * @param pos the changed position packed by {@link LightPositions#pack}
     */
    public void invalidate(LightAccess access, long pos) {
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

        for (Direction direction : DIRECTIONS) {
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();

            if (ny < minHeight || ny >= maxHeight || !access.isLoaded(nx, nz)) {
                continue;
            }

            // Shape changes can remove support even when the former arrival is no longer observable.
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

    /**
     * Queues an already-lit position to spread its current light.
     */
    public void source(long pos) {
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
    public boolean run(LightAccess access, int[] remaining) {
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

        for (Direction direction : DIRECTIONS) {
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

            // Arrivals never exceed the spread level, so a neighbor already
            // above it keeps its light and only needs a fresh increase visit.
            if (current <= oldLevel && current <= this.channel.arrive(states, fromPrimary, fromSecondary, toPrimary, toSecondary, direction, oldLevel)) {
                int own = this.channel.ownValue(states, toPrimary, toSecondary, ny, maxHeight);

                // Unchanged emitters must not repeatedly withdraw each other's support.
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

        for (Direction direction : DIRECTIONS) {
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();

            if (ny < minHeight || ny >= maxHeight || !access.isLoaded(nx, nz)) {
                continue;
            }

            // Arrivals never exceed the source level, so a neighbor already
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
