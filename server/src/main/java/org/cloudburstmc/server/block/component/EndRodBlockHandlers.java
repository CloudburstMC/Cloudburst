package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.util.Direction;

@UtilityClass
public class EndRodBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, _, face, _) -> {
        Direction opposite = face.getOpposite();
        BlockState behind = block.getSide(opposite).getState();
        boolean joinsTip = behind.getType() == BlockTypes.END_ROD && behind.ensureTrait(BlockTraits.FACING_DIRECTION) == face;
        return state.withTrait(BlockTraits.FACING_DIRECTION, joinsTip ? opposite : face);
    };
}
