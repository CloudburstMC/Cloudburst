package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;

public class FenceGatePlaceHandler implements PlaceBlockHandler {

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        if (player == null) {
            return false;
        }

        Direction horizontal = player.getHorizontalDirection();
        CardinalDirection cardinal = horizontal.getCardinalDirection();
        blockState = blockState.withTrait(BlockTraits.CARDINAL_DIRECTION, cardinal);
        blockState = blockState.withTrait(BlockTraits.IS_IN_WALL, shouldBeLowered(blockPosition, horizontal, (CloudLevel) player.getLevel()));

        return player.getLevel().setBlockState(blockPosition, blockState, true, true);
    }

    private boolean shouldBeLowered(Vector3i blockPosition, Direction facing, CloudLevel level) {
        Direction left = facing.rotateCounterClockwise();
        Vector3i leftPosition = left.relative(blockPosition);
        Vector3i rightPosition = left.getOpposite().relative(blockPosition);
        return level.getBlockState(leftPosition.getX(), leftPosition.getY(), leftPosition.getZ()).getType().hasTag(BlockTags.WALLS)
                || level.getBlockState(rightPosition.getX(), rightPosition.getY(), rightPosition.getZ()).getType().hasTag(BlockTags.WALLS);
    }
}
