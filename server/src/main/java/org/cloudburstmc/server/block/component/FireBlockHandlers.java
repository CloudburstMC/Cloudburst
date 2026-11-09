package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.util.Direction;

@UtilityClass
public class FireBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, clickPosition) -> placementState(block);

    public static final ComplexBlockHandler ON_ADDED = FireBlockHandlers::updateConnections;

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        if (!block.getLiquid().isEmpty()) {
            return false;
        }

        if (block.getSide(Direction.DOWN).isFaceSturdy(Direction.UP, SupportType.FULL)) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            Block neighbor = block.getSide(direction);
            if (neighbor.getLiquid().isEmpty() && neighbor.getState().getFlameOdds() > 0) {
                return true;
            }
        }

        return false;
    };

    public static final SurviveBlockHandler SOUL_CAN_SURVIVE = block ->
            block.getLiquid().isEmpty() && isSoulFireBase(block.getSideState(Direction.DOWN));

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (!block.requireComponent(BlockComponents.CAN_SURVIVE).execute(block)) {
            block.set(BlockStates.AIR);
        } else if (block.getState().getType() == BlockTypes.FIRE) {
            updateConnections(block);
        }
    };

    public static BlockState placementState(Block block) {
        Block below = block.getSide(Direction.DOWN);
        if (isSoulFireBase(below.getState())) {
            return BlockStates.SOUL_FIRE;
        }

        if (canBurn(below) || below.isFaceSturdy(Direction.UP, SupportType.FULL)) {
            return BlockStates.FIRE;
        }

        return BlockStates.FIRE
                .withTrait(BlockTraits.CONNECTION_EAST, canBurn(block.getSide(Direction.EAST)))
                .withTrait(BlockTraits.CONNECTION_NORTH, canBurn(block.getSide(Direction.NORTH)))
                .withTrait(BlockTraits.CONNECTION_SOUTH, canBurn(block.getSide(Direction.SOUTH)))
                .withTrait(BlockTraits.CONNECTION_UP, canBurn(block.getSide(Direction.UP)))
                .withTrait(BlockTraits.CONNECTION_WEST, canBurn(block.getSide(Direction.WEST)));
    }

    private static void updateConnections(Block block) {
        BlockState state = block.getState();
        BlockState connected = placementState(block);

        if (connected.getType() == BlockTypes.FIRE) {
            connected = connected.withTrait(BlockTraits.AGE, state.ensureTrait(BlockTraits.AGE));
        }

        if (connected != state) {
            block.set(connected, false, true);
        }
    }

    private static boolean canBurn(Block block) {
        return block.getLiquid().isEmpty() && block.getState().getFlameOdds() > 0;
    }

    private static boolean isSoulFireBase(BlockState state) {
        return state.getType() == BlockTypes.SOUL_SAND || state.getType() == BlockTypes.SOUL_SOIL;
    }
}
