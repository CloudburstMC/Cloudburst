package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;

/**
 * Simulation rules shared by the source and flowing block types of a liquid.
 */
enum LiquidFamily {
    WATER(BlockTypes.WATER, BlockTypes.FLOWING_WATER, 1, 4, 5, true, true),
    LAVA(BlockTypes.LAVA, BlockTypes.FLOWING_LAVA, 2, 2, 30, false, false);

    private final BlockType sourceType;
    private final BlockType flowingType;
    private final int defaultDropOff;
    private final int defaultSlopeDistance;
    private final int defaultTickDelay;
    private final boolean formsSources;
    private final boolean canBeContained;

    LiquidFamily(BlockType sourceType, BlockType flowingType, int defaultDropOff, int defaultSlopeDistance,
                 int defaultTickDelay, boolean formsSources, boolean canBeContained) {
        this.sourceType = sourceType;
        this.flowingType = flowingType;
        this.defaultDropOff = defaultDropOff;
        this.defaultSlopeDistance = defaultSlopeDistance;
        this.defaultTickDelay = defaultTickDelay;
        this.formsSources = formsSources;
        this.canBeContained = canBeContained;
    }

    static LiquidFamily of(LiquidState state) {
        if (state.getType().isSameFamily(LiquidTypes.WATER)) {
            return WATER;
        }

        if (state.getType().isSameFamily(LiquidTypes.LAVA)) {
            return LAVA;
        }

        throw new IllegalArgumentException("Unsupported liquid type: " + state.getType());
    }

    boolean matches(LiquidState state) {
        return !state.isEmpty() && of(state) == this;
    }

    boolean formsSources() {
        return this.formsSources;
    }

    boolean canBeContained() {
        return this.canBeContained;
    }

    LiquidState source() {
        return LiquidState.of(this.sourceType.getDefaultState().withTrait(BlockTraits.LIQUID_DEPTH, 0));
    }

    LiquidState falling() {
        return LiquidState.of(this.flowingType.getDefaultState().withTrait(BlockTraits.LIQUID_DEPTH, 8));
    }

    LiquidState flowing(int amount) {
        return LiquidState.of(this.flowingType.getDefaultState().withTrait(BlockTraits.LIQUID_DEPTH, 8 - amount));
    }

    LiquidState nextState(CloudLevel level, Vector3i position, LiquidState current) {
        return current.isSource()
                ? current : LiquidBlockHandlers.nextLiquid(level, position, this);
    }

    int dropOff(CloudLevel level) {
        return this == LAVA && isNether(level) ? 1 : this.defaultDropOff;
    }

    int slopeDistance(CloudLevel level) {
        return this == LAVA && isNether(level) ? 4 : this.defaultSlopeDistance;
    }

    int tickDelay(CloudLevel level, Vector3i position) {
        if (this == WATER && hasHorizontalLava(level, position)) {
            return level.getWaterOverLavaFlowSpeed();
        }

        return this == LAVA && isNether(level) ? 10 : this.defaultTickDelay;
    }

    int spreadDelay(CloudLevel level, Vector3i position, LiquidState oldState, LiquidState newState) {
        int delay = tickDelay(level, position);
        if (this == LAVA && !oldState.isFalling() && !newState.isFalling()
                && newState.getAmount() > oldState.getAmount()
                && level.getRandom().nextInt(4) != 0) {
            delay *= 4;
        }

        return delay;
    }

    boolean harden(CloudLevel level, Vector3i position) {
        if (this != LAVA) {
            return false;
        }

        Block below = LiquidBlockHandlers.loadedBlock(level, Direction.DOWN.relative(position));
        if (below != null && below.getState().getType() == BlockTypes.SOUL_SOIL) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                Block adjacent = LiquidBlockHandlers.loadedBlock(level, direction.relative(position));
                if (adjacent != null && adjacent.getState().getType() == BlockTypes.BLUE_ICE) {
                    return formWithSound(level, position, BlockStates.BASALT);
                }
            }
        }

        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN) {
                continue;
            }

            Block adjacent = LiquidBlockHandlers.loadedBlock(level, direction.relative(position));
            if (adjacent != null && WATER.matches(adjacent.getLiquid())) {
                BlockState result = level.getBlock(position).getLiquid().isSource()
                        ? BlockStates.OBSIDIAN : BlockStates.COBBLESTONE;
                return formWithSound(level, position, result);
            }
        }

        return false;
    }

    private static boolean isNether(CloudLevel level) {
        return level.getDimension() == CloudLevel.DIMENSION_NETHER;
    }

    private static boolean hasHorizontalLava(CloudLevel level, Vector3i position) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Block adjacent = LiquidBlockHandlers.loadedBlock(level, direction.relative(position));
            if (adjacent != null && LAVA.matches(adjacent.getLiquid())) {
                return true;
            }
        }

        return false;
    }

    private static boolean formWithSound(CloudLevel level, Vector3i position, BlockState state) {
        if (!LiquidBlockHandlers.form(level, position, state)) {
            return false;
        }

        level.addSound(position, Sound.RANDOM_FIZZ, 0.5f, 2.6f);
        return true;
    }
}
