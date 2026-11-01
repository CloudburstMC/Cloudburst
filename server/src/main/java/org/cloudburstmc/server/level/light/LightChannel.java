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
     * The six propagation directions, cached to avoid cloning the enum array
     * on every cell visit.
     */
    static final Direction[] DIRECTIONS = Direction.values();

    /**
     * Light reaching the neighbour of {@code (x, y, z)} in {@code direction}
     * when {@code (x, y, z)} carries {@code level}.
     *
     * <p>The shared face blocks everything when the face shapes of both layers
     * of both blocks together cover it, so complementary partial faces seal
     * it together. Block light is then 0 when the target dampens or emits 15 or more,
     * otherwise {@code level} minus at least 1. Sky light is 0 when the target
     * dampens 15 or more, 15 minus dampening moving down at full strength,
     * otherwise {@code level} minus at least 1.
     *
     * @return the arriving light, at least 0
     */
    int arrive(LightAccess access, int x, int y, int z, Direction direction, int level) {
        return arrive(access.states(), access.primary(x, y, z), access.secondary(x, y, z),
                access.primary(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ()),
                access.secondary(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ()),
                direction, level);
    }

    /**
     * Light reaching the neighbour in {@code direction} from a cell carrying
     * {@code level}, with both cells' states already read.
     *
     * <p>Reads the target states once per cell instead of once per neighbour.
     *
     * @return the arriving light, at least 0
     */
    int arrive(LightStateCache states, BlockState fromPrimary, BlockState fromSecondary,
            BlockState toPrimary, BlockState toSecondary, Direction direction, int level) {
        LightStateCache.Entry fromPrimaryEntry = states.entryFor(fromPrimary);
        LightStateCache.Entry fromSecondaryEntry = states.entryFor(fromSecondary);
        LightStateCache.Entry toPrimaryEntry = states.entryFor(toPrimary);
        LightStateCache.Entry toSecondaryEntry = states.entryFor(toSecondary);
        if (states.occludes(fromPrimaryEntry, fromSecondaryEntry, toPrimaryEntry, toSecondaryEntry, direction)) {
            return 0;
        }
        int opacity = states.opacity(toPrimaryEntry, toSecondaryEntry);
        if (this == BLOCK) {
            if (opacity >= 15 || states.emission(toPrimaryEntry, toSecondaryEntry) >= 15) {
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
     * Light a cell holds without neighbours, with its states already read.
     */
    int ownValue(LightStateCache states, BlockState primary, BlockState secondary, int y, int maxHeight) {
        if (this == BLOCK) {
            return states.emission(primary, secondary);
        }
        if (y == maxHeight - 1
                && !states.occludes(BlockStates.AIR, BlockStates.AIR, primary, secondary, Direction.DOWN)) {
            return Math.max(0, 15 - states.opacity(primary, secondary));
        }
        return 0;
    }

    /**
     * Settles a cell to the maximum of its own value and the best arrival from
     * loaded in-height neighbours, matching what propagation converges to.
     */
    int recompute(LightAccess access, int x, int y, int z) {
        LightStateCache states = access.states();
        BlockState primary = access.primary(x, y, z);
        BlockState secondary = access.secondary(x, y, z);
        return Math.clamp(Math.max(ownValue(states, primary, secondary, y, access.maxHeight()),
                maxArrival(access, states, primary, secondary, x, y, z)), 0, 15);
    }

    private int maxArrival(LightAccess access, LightStateCache states,
            BlockState primary, BlockState secondary, int x, int y, int z) {
        int best = 0;
        int minHeight = access.minHeight();
        int maxHeight = access.maxHeight();
        if (this == SKY && y == maxHeight - 1) {
            best = arrive(states, BlockStates.AIR, BlockStates.AIR, primary, secondary, Direction.DOWN, 15);
        }
        for (int i = 0; i < DIRECTIONS.length; i++) {
            Direction direction = DIRECTIONS[i];
            int nx = x + direction.getStepX();
            int ny = y + direction.getStepY();
            int nz = z + direction.getStepZ();
            if (ny < minHeight || ny >= maxHeight || !access.isLoaded(nx, nz)) {
                continue;
            }
            int arrival = arrive(states, access.primary(nx, ny, nz), access.secondary(nx, ny, nz),
                    primary, secondary, direction.getOpposite(), access.light(nx, ny, nz));
            if (arrival > best) {
                best = arrival;
            }
        }
        return best;
    }

}
