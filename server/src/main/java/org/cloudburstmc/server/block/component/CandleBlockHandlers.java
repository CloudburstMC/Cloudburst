package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.ReplaceBlockHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.util.BlockSupport;

@UtilityClass
public class CandleBlockHandlers {

    public static final ReplaceBlockHandler CAN_BE_REPLACED = (block, replacement, player, face, click) ->
            block.getState().getType() == replacement.getType() && block.getState().ensureTrait(BlockTraits.CANDLES) < 3;

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) ->
            block.getState().getType() == state.getType() ? block.getState().incrementTrait(BlockTraits.CANDLES)
                    : DefaultPlacementStateHandler.INSTANCE.execute(state, block, player, face, click);

    public static final SurviveBlockHandler CAN_SURVIVE = block ->
            block.getRelativeState(0, -1, 0).getType() != BlockTypes.CAKE
                    && BlockSupport.canSupportCenter(block.getLevel(), block.getPosition().add(0, -1, 0), Direction.UP);
}
