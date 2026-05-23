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
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        if (player == null) {
            return false;
        }

        Direction horizontal = player.getHorizontalDirection();
        CardinalDirection cardinal = horizontal.getCardinalDirection();
        blockState = blockState.withTrait(BlockTraits.CARDINAL_DIRECTION, cardinal);
        blockState = blockState.withTrait(BlockTraits.IS_IN_WALL, shouldBeLowered(pos, horizontal, (CloudLevel) player.getLevel()));

        return player.getLevel().setBlockState(pos, blockState, true, true);
    }

    private boolean shouldBeLowered(Vector3i pos, Direction facing, CloudLevel level) {
        Direction left = facing.rotateCounterClockwise();
        Vector3i leftPos = left.relative(pos);
        Vector3i rightPos = left.getOpposite().relative(pos);
        return level.getBlockState(leftPos.getX(), leftPos.getY(), leftPos.getZ()).getType().hasTag(BlockTags.WALLS)
                || level.getBlockState(rightPos.getX(), rightPos.getY(), rightPos.getZ()).getType().hasTag(BlockTags.WALLS);
    }
}
