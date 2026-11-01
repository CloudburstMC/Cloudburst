package org.cloudburstmc.server.level.light;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;

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
        int own = this.channel.ownValue(access, x, y, z);
        if (own != old) {
            access.setLight(x, y, z, own);
        }
        if (old != own) {
            this.decreasePositions.enqueue(pos);
            this.decreaseLevels.enqueue(old);
        }
        LightChannel.forEachNeighbour(x, y, z, (nx, ny, nz, direction) -> {
            if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                return;
            }
            int current = access.light(nx, ny, nz);
            int relaxed = this.channel.recompute(access, nx, ny, nz);
            if (relaxed < current) {
                access.setLight(nx, ny, nz, relaxed);
                this.decreasePositions.enqueue(LightPositions.pack(nx, ny, nz));
                this.decreaseLevels.enqueue(current);
            }
            this.increasePositions.enqueue(LightPositions.pack(nx, ny, nz));
        });
        if (own > 0) {
            this.increasePositions.enqueue(pos);
        }
    }

    void source(long pos) {
        this.increasePositions.enqueue(pos);
    }

    boolean run(LightAccess access, int maxSteps) {
        int[] remaining = {maxSteps};
        boolean drained = run(access, remaining);
        return drained;
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
        LightChannel.forEachNeighbour(x, y, z, (nx, ny, nz, direction) -> {
            if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                return;
            }
            int current = access.light(nx, ny, nz);
            if (current <= 0) {
                return;
            }
            if (current <= this.channel.arrive(access, x, y, z, direction, oldLevel)) {
                int own = this.channel.ownValue(access, nx, ny, nz);
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
        });
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
        LightChannel.forEachNeighbour(x, y, z, (nx, ny, nz, direction) -> {
            if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                return;
            }
            int arrival = this.channel.arrive(access, x, y, z, direction, level);
            if (arrival > access.light(nx, ny, nz)) {
                access.setLight(nx, ny, nz, arrival);
                this.increasePositions.enqueue(LightPositions.pack(nx, ny, nz));
            }
        });
    }
}
