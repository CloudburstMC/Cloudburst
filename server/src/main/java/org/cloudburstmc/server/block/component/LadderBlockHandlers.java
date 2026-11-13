package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.SupportType;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.util.Direction;

@UtilityClass
public class LadderBlockHandlers {

    public static final SurviveBlockHandler CAN_SURVIVE = block -> canAttach(block, block.getState().ensureTrait(BlockTraits.FACING_DIRECTION));

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) -> {
        Block support = block.getSide(face.getOpposite());
        if (support.getState().getType() == BlockTypes.LADDER
                && support.getState().ensureTrait(BlockTraits.FACING_DIRECTION) == face) {
            return null;
        }

        if (canAttach(block, face)) {
            return state.withTrait(BlockTraits.FACING_DIRECTION, face);
        }

        Direction facing = player == null ? state.ensureTrait(BlockTraits.FACING_DIRECTION) : player.getHorizontalDirection().getOpposite();
        if (canAttach(block, facing)) {
            return state.withTrait(BlockTraits.FACING_DIRECTION, facing);
        }

        for (Direction candidate : Direction.Plane.HORIZONTAL) {
            if (canAttach(block, candidate)) {
                return state.withTrait(BlockTraits.FACING_DIRECTION, candidate);
            }
        }


        return null;
    };

    private static boolean canAttach(Block block, Direction facing) {
        return facing.getAxis().isHorizontal() && block.getSide(facing.getOpposite()).isFaceSturdy(facing, SupportType.FULL);
    }
}
