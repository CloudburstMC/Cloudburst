package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.PlacementSupport;

public class TorchPlaceHandler implements PlaceBlockHandler {

    private static final Direction[] FALLBACK_FACES = {
            Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.UP
    };

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        Direction attachFace = face == Direction.DOWN ? findValidFace(player.getLevel(), blockPosition) : face;
        if (attachFace == null || !canSupportTorch(player.getLevel(), blockPosition, attachFace)) {
            attachFace = findValidFace(player.getLevel(), blockPosition);
        }

        if (attachFace == null) {
            return false;
        }

        Direction torchDirection = attachFace == Direction.UP ? Direction.DOWN : attachFace.getOpposite();
        blockState = blockState.withTrait(BlockTraits.TORCH_DIRECTION, torchDirection);
        return player.getLevel().setBlockState(blockPosition, blockState, true, true);
    }

    private Direction findValidFace(Level level, Vector3i blockPosition) {
        for (Direction candidate : FALLBACK_FACES) {
            if (canSupportTorch(level, blockPosition, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean canSupportTorch(Level level, Vector3i blockPosition, Direction attachFace) {
        return PlacementSupport.hasCenterFaceSupport(level, blockPosition, attachFace);
    }
}
