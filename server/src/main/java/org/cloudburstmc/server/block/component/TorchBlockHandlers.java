package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.PlacementSupport;

@UtilityClass
public class TorchBlockHandlers {

    private static final Direction[] FALLBACK_FACES = {
            Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.UP
    };

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) -> {
        if (canAttach(block.getLevel(), block.getPosition(), face)) {
            return withAttachment(state, face);
        }

        for (Direction candidate : FALLBACK_FACES) {
            if (canAttach(block.getLevel(), block.getPosition(), candidate)) {
                return withAttachment(state, candidate);
            }
        }

        return null;
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> canAttach(block.getLevel(), block.getPosition(), supportFace(block.getState()));

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        Vector3i pos = block.getPosition();
        Direction supportFace = supportFace(block.getState());
        Vector3i supportPos = PlacementSupport.supportPosition(pos, supportFace);

        if (!neighbor.getPosition().equals(supportPos)) {
            return;
        }

        if (CAN_SURVIVE.execute(block)) {
            return;
        }

        block.getLevel().breakBlock(pos, null, null, true);
    };

    public static boolean canAttach(Level level, Vector3i position, Direction face) {
        if (face == Direction.DOWN) {
            return false;
        }

        return face == Direction.UP ? PlacementSupport.hasCenterFaceSupport(level, position, face) : PlacementSupport.hasFullFaceSupport(level, position, face);
    }

    private static Direction supportFace(BlockState state) {
        Direction direction = state.ensureTrait(BlockTraits.TORCH_DIRECTION);
        return direction.getAxis() == Direction.Axis.Y ? direction : direction.getOpposite();
    }

    private static BlockState withAttachment(BlockState state, Direction face) {
        return state.withTrait(BlockTraits.TORCH_DIRECTION, face == Direction.UP ? Direction.UP : face.getOpposite());
    }
}
