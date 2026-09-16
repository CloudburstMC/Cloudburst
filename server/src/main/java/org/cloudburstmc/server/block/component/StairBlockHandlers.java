package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.StairShape;
import org.cloudburstmc.math.vector.Vector3i;

@UtilityClass
public class StairBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, clickPosition) -> {
        if (player == null) {
            return state;
        }

        boolean upsideDown = face == Direction.DOWN || face != Direction.UP && clickPosition.getY() > 0.5f;
        BlockState placementState = state
                .withTrait(BlockTraits.DIRECTION, player.getHorizontalDirection())
                .withTrait(BlockTraits.IS_UPSIDE_DOWN, upsideDown);
        return resolveShape(block.getLevel(), block.getPosition(), placementState);
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        Vector3i position = block.getPosition();
        if (neighbor.getY() != position.getY()) {
            return;
        }

        BlockState state = block.getState();
        BlockState updatedState = resolveShape(block.getLevel(), position, state);
        if (updatedState != state) {
            block.set(updatedState, false, true);
        }
    };

    public static BlockState resolveShape(Level level, Vector3i position, BlockState state) {
        Direction facing = state.ensureTrait(BlockTraits.DIRECTION);
        boolean upsideDown = state.ensureTrait(BlockTraits.IS_UPSIDE_DOWN);

        BlockState forwardState = level.getBlockState(facing.relative(position));
        if (isMatchingStair(forwardState, upsideDown)) {
            Direction forwardFacing = forwardState.ensureTrait(BlockTraits.DIRECTION);
            if (forwardFacing.getAxis() != facing.getAxis() && canTakeShape(level, position, state, forwardFacing.getOpposite())) {
                StairShape shape = forwardFacing == facing.rotateCounterClockwise() ? StairShape.OUTER_LEFT : StairShape.OUTER_RIGHT;
                return state.withTrait(BlockTraits.STAIR_SHAPE, shape);
            }
        }

        BlockState backwardState = level.getBlockState(facing.getOpposite().relative(position));
        if (isMatchingStair(backwardState, upsideDown)) {
            Direction backwardFacing = backwardState.ensureTrait(BlockTraits.DIRECTION);
            if (backwardFacing.getAxis() != facing.getAxis() && canTakeShape(level, position, state, backwardFacing)) {
                StairShape shape = backwardFacing == facing.rotateCounterClockwise() ? StairShape.INNER_LEFT : StairShape.INNER_RIGHT;
                return state.withTrait(BlockTraits.STAIR_SHAPE, shape);
            }
        }

        return state.withTrait(BlockTraits.STAIR_SHAPE, StairShape.NONE);
    }

    private static boolean canTakeShape(Level level, Vector3i position, BlockState state, Direction direction) {
        BlockState neighbor = level.getBlockState(direction.relative(position));
        return !isMatchingStair(neighbor, state.ensureTrait(BlockTraits.IS_UPSIDE_DOWN)) || neighbor.ensureTrait(BlockTraits.DIRECTION) != state.ensureTrait(BlockTraits.DIRECTION);
    }

    private static boolean isMatchingStair(BlockState state, boolean upsideDown) {
        return state.getTraits().containsKey(BlockTraits.STAIR_SHAPE) && state.ensureTrait(BlockTraits.IS_UPSIDE_DOWN) == upsideDown;
    }
}
