package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.block.util.TripwireCalculator;
import org.cloudburstmc.server.level.CloudLevel;

public class TripwireHookPlaceHandler implements PlaceBlockHandler {

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        if (!face.getAxis().isHorizontal()) {
            return false;
        }

        if (!PlacementSupport.hasFullFaceSupport(player.getLevel(), blockPosition, face)) {
            return false;
        }

        blockState = blockState.withTrait(BlockTraits.DIRECTION, face);
        if (!player.getLevel().setBlockState(blockPosition, blockState, true, true)) {
            return false;
        }

        TripwireCalculator.calculateState((CloudLevel) player.getLevel(), blockPosition, blockState, false);
        return true;
    }
}
