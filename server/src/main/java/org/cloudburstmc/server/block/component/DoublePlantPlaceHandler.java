package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

public final class DoublePlantPlaceHandler implements PlaceBlockHandler {

    @Override
    public boolean execute(BlockState state, Player player, Vector3i position, Direction face, Vector3f clickPosition) {
        if (player == null) {
            return false;
        }

        Level level = player.getLevel();
        Vector3i upperPosition = position.add(0, 1, 0);
        if (!level.getBlockState(upperPosition).isReplaceable()) {
            return false;
        }

        BlockState previous = level.getBlockState(position);
        if (!level.setBlockState(position, VegetationBlockHandlers.lowerHalf(state), false, false)) {
            return false;
        }

        if (!level.setBlockState(upperPosition, VegetationBlockHandlers.upperHalf(state), false, true)) {
            level.setBlockState(position, previous, false, true);
            return false;
        }

        return true;
    }
}
