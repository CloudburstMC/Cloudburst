package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;

@UtilityClass
public class IceCrystalBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) ->
            state.withTrait(BlockTraits.BLOCK_FACE, face);

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        Direction facing = block.getState().ensureTrait(BlockTraits.BLOCK_FACE);
        Vector3i support = facing.getOpposite().relative(block.getPosition());
        return support.getY() >= block.getLevel().getMinHeight() && support.getY() < block.getLevel().getMaxHeight()
                && block.getLevel().getLoadedBlock(support) != null && BlockSupport.isFaceSturdy(block.getLevel(), support, facing);
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        Direction facing = block.getState().ensureTrait(BlockTraits.BLOCK_FACE);
        if (neighbor.getPosition().equals(facing.getOpposite().relative(block.getPosition()))) {
            VegetationBlockHandlers.checkSurvival(CAN_SURVIVE).execute(block, neighbor);
        }
    };
}
