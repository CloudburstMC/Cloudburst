package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.util.Direction;

/**
 * The two light channels, propagated independently of each other.
 *
 * <p>Block light settles at the maximum of a cell's emission and the best
 * arrival from its neighbours. Sky light has no emission: it falls from the
 * virtual air above the top layer and settles at the best arrival instead.
 */
enum LightChannel {
    BLOCK,
    SKY;

    /**
     * Light reaching the neighbour of {@code (x, y, z)} in {@code direction}
     * when {@code (x, y, z)} carries {@code level}.
     *
     * <p>The shared face blocks everything when either side's shape seals it.
     * Block light is then 0 when the target dampens or emits 15 or more,
     * otherwise {@code level} minus at least 1. Sky light is 0 when the target
     * dampens 15 or more, 15 minus dampening moving down at full strength,
     * otherwise {@code level} minus at least 1.
     *
     * @return the arriving light, at least 0
     */
    int arrive(LightAccess access, int x, int y, int z, Direction direction, int level) {
        LightStateCache states = access.states();
        BlockState fromPrimary = access.primary(x, y, z);
        BlockState fromSecondary = access.secondary(x, y, z);
        int nx = x + direction.getStepX();
        int ny = y + direction.getStepY();
        int nz = z + direction.getStepZ();
        BlockState toPrimary = access.primary(nx, ny, nz);
        BlockState toSecondary = access.secondary(nx, ny, nz);
        if (states.occludes(fromPrimary, fromSecondary, toPrimary, toSecondary, direction)) {
            return 0;
        }
        int opacity = states.opacity(toPrimary, toSecondary);
        if (this == BLOCK) {
            if (opacity >= 15 || states.emission(toPrimary, toSecondary) >= 15) {
                return 0;
            }
            return Math.max(0, level - Math.max(1, opacity));
        }
        if (opacity >= 15) {
            return 0;
        }
        if (direction == Direction.DOWN && level == 15) {
            return 15 - opacity;
        }
        return Math.max(0, level - Math.max(1, opacity));
    }

    /**
     * Light a cell holds without neighbours: emission for block light, the
     * arrival from the virtual air above for the top layer of sky light,
     * otherwise 0.
     */
    int ownValue(LightAccess access, int x, int y, int z) {
        if (this == BLOCK) {
            return access.states().emission(access.primary(x, y, z), access.secondary(x, y, z));
        }
        if (y == access.maxHeight() - 1
                && !access.states().occludes(BlockStates.AIR, BlockStates.AIR,
                        access.primary(x, y, z), access.secondary(x, y, z), Direction.DOWN)) {
            return Math.max(0, 15 - access.states().opacity(access.primary(x, y, z), access.secondary(x, y, z)));
        }
        return 0;
    }

    /**
     * Settles a cell to the maximum of its own value and the best arrival from
     * loaded in-height neighbours, matching what propagation converges to.
     */
    int recompute(LightAccess access, int x, int y, int z) {
        return Math.clamp(Math.max(ownValue(access, x, y, z), maxArrival(access, x, y, z)), 0, 15);
    }

    private int maxArrival(LightAccess access, int x, int y, int z) {
        int best = 0;
        if (this == SKY && y == access.maxHeight() - 1) {
            best = arrive(access, x, y + 1, z, Direction.DOWN, 15);
        }
        for (Direction direction : Direction.values()) {
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();
            if (ny < access.minHeight() || ny >= access.maxHeight() || !access.isLoaded(nx, nz)) {
                continue;
            }
            int arrival = arrive(access, nx, ny, nz, direction.getOpposite(), access.light(nx, ny, nz));
            if (arrival > best) {
                best = arrival;
            }
        }
        return best;
    }

    /**
     * Visits the six neighbours of {@code (x, y, z)}, passing the direction
     * from the cell to each neighbour. Callers skip unloaded and out-of-height
     * neighbours themselves.
     */
    static void forEachNeighbour(int x, int y, int z, NeighbourCell cell) {
        for (Direction direction : Direction.values()) {
            cell.visit(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ(), direction);
        }
    }

    @FunctionalInterface
    interface NeighbourCell {
        void visit(int x, int y, int z, Direction direction);
    }
}
