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

public class TripwireBlockPlaceHandler implements PlaceBlockHandler {

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        if (player == null) {
            return false;
        }

        CloudLevel level = (CloudLevel) player.getLevel();
        boolean suspended = !PlacementSupport.hasFloorSupport(level, blockPosition);

        blockState = blockState.withTrait(BlockTraits.IS_SUSPENDED, suspended);
        if (!level.setBlockState(blockPosition, blockState, true, true)) {
            return false;
        }

        TripwireCalculator.notifyHooksAround(level, blockPosition);
        return true;
    }
}
