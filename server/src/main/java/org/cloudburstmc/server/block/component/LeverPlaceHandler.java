package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.LeverDirection;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.PlacementSupport;

public class LeverPlaceHandler implements PlaceBlockHandler {

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        if (!canAttachToFace(player.getLevel(), blockPosition, face)) {
            return false;
        }

        Direction horizontal = player.getHorizontalDirection();
        LeverDirection leverDirection = LeverDirection.forDirection(face, horizontal);
        blockState = blockState.withTrait(BlockTraits.LEVER_DIRECTION, leverDirection);
        return player.getLevel().setBlockState(blockPosition, blockState, true, true);
    }

    private boolean canAttachToFace(Level level, Vector3i blockPosition, Direction face) {
        return PlacementSupport.hasFullFaceSupport(level, blockPosition, face);
    }
}
