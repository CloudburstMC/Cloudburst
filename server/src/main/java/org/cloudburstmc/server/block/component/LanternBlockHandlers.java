package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;

@UtilityClass
public class LanternBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, _) -> {
        Direction preferred = switch (face) {
            case DOWN -> Direction.UP;
            case UP -> Direction.DOWN;
            default -> player != null && player.getPitch() < 0 ? Direction.UP : Direction.DOWN;
        };

        if (hasSupport(block, preferred)) {
            return state.withTrait(BlockTraits.IS_HANGING, preferred == Direction.UP);
        }

        Direction fallback = preferred.getOpposite();
        return hasSupport(block, fallback) ? state.withTrait(BlockTraits.IS_HANGING, fallback == Direction.UP) : null;
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> hasSupport(block, supportSide(block));

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        Direction side = supportSide(block);
        if (neighbor.getPosition().equals(side.relative(block.getPosition())) && !hasSupport(block, side)) {
            block.getLevel().breakBlock(block.getPosition(), null, null, true);
        }
    };

    private static Direction supportSide(Block block) {
        return block.getState().ensureTrait(BlockTraits.IS_HANGING) ? Direction.UP : Direction.DOWN;
    }

    private static boolean hasSupport(Block block, Direction side) {
        Vector3i support = side.relative(block.getPosition());
        return BlockSupport.canSupportCenter(block.getLevel(), support, side.getOpposite());
    }
}
