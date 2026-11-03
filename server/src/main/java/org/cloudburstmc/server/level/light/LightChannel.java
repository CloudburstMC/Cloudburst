package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.util.Direction;

/**
 * Attenuation and source rules for independently propagated block and sky light.
 */
public enum LightChannel {
    BLOCK,
    SKY;

    private static final Direction[] DIRECTIONS = Direction.values();

    /**
     * Returns light arriving at the adjacent cell in the given direction.
     * Combined block faces and the target's opacity determine attenuation.
     */
    public int arrive(LightAccess access, int x, int y, int z, Direction direction, int level) {
        return arrive(
                access.states(), access.primary(x, y, z), access.secondary(x, y, z),
                access.primary(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ()),
                access.secondary(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ()),
                direction,
                level
        );
    }

    /**
     * Returns light arriving from the source layers at the target layers.
     * The direction points from source to target.
     */
    public int arrive(LightStateCache states, BlockState fromPrimary, BlockState fromSecondary, BlockState toPrimary, BlockState toSecondary, Direction direction, int level) {
        LightStateCache.Entry fromPrimaryEntry = states.entryFor(fromPrimary);
        LightStateCache.Entry fromSecondaryEntry = states.entryFor(fromSecondary);
        LightStateCache.Entry toPrimaryEntry = states.entryFor(toPrimary);
        LightStateCache.Entry toSecondaryEntry = states.entryFor(toSecondary);

        if (states.occludes(fromPrimaryEntry, fromSecondaryEntry, toPrimaryEntry, toSecondaryEntry, direction)) {
            return 0;
        }

        int opacity = states.opacity(toPrimaryEntry, toSecondaryEntry);
        if (opacity >= 15) {
            return 0;
        }

        if (this == BLOCK && states.emission(toPrimaryEntry, toSecondaryEntry) >= 15) {
            return 0;
        }

        if (this == SKY && direction == Direction.DOWN && level == 15) {
            return 15 - opacity;
        }

        return Math.max(0, level - Math.max(1, opacity));
    }

    /**
     * Returns the cell's own emission or direct top-boundary skylight.
     */
    public int ownValue(LightStateCache states, BlockState primary, BlockState secondary, int y, int maxHeight) {
        if (this == BLOCK) {
            return states.emission(primary, secondary);
        }

        if (y == maxHeight - 1 && !states.occludes(BlockStates.AIR, BlockStates.AIR, primary, secondary, Direction.DOWN)) {
            return Math.max(0, 15 - states.opacity(primary, secondary));
        }

        return 0;
    }

    /**
     * Returns the cell's light implied by its own value and available neighbors.
     */
    public int recompute(LightAccess access, int x, int y, int z) {
        LightStateCache states = access.states();
        BlockState primary = access.primary(x, y, z);
        BlockState secondary = access.secondary(x, y, z);

        return Math.clamp(Math.max(ownValue(states, primary, secondary, y, access.maxHeight()),
                maxArrival(access, states, primary, secondary, x, y, z)), 0, 15);
    }

    private int maxArrival(LightAccess access, LightStateCache states, BlockState primary, BlockState secondary, int x, int y, int z) {
        int best = 0;
        int minHeight = access.minHeight();
        int maxHeight = access.maxHeight();

        if (this == SKY && y == maxHeight - 1) {
            best = arrive(states, BlockStates.AIR, BlockStates.AIR, primary, secondary, Direction.DOWN, 15);
        }

        for (Direction direction : DIRECTIONS) {
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
