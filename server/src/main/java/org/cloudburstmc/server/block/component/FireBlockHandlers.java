package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.util.Direction;

@UtilityClass
public class FireBlockHandlers {

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
        }
    };

    public static BlockState placementState(Block block) {
        return isSoulFireBase(block.getSideState(Direction.DOWN)) ? BlockStates.SOUL_FIRE : BlockStates.FIRE;
    }

    private static boolean isSoulFireBase(BlockState state) {
        return state.getType() == BlockTypes.SOUL_SAND || state.getType() == BlockTypes.SOUL_SOIL;
    }
}
