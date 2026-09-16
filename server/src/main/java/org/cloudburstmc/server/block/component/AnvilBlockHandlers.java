package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlacementStateHandler;

@UtilityClass
public class AnvilBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, clickPosition) -> {
        if (player == null) {
            return state;
        }

        return state.withTrait(BlockTraits.CARDINAL_DIRECTION, player.getHorizontalDirection().rotateClockwise().getCardinalDirection());
    };
}
