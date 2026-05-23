package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class DoorPlaceHandler implements PlaceBlockHandler {

    private final CloudBlockRegistry registry;

    public DoorPlaceHandler(CloudBlockRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i pos, Direction face, Vector3f clickPos) {
        if (player == null) {
            return false;
        }

        if (face != Direction.UP) {
            return false;
        }

        Level level = player.getLevel();
        BlockState below = level.getBlockState(pos.getX(), pos.getY() - 1, pos.getZ());
        if (!registry.getComponents(below.getType()).get(BlockComponents.SOLID).get()) {
            return false;
        }

        Vector3i upperPos = pos.add(0, 1, 0);
        BlockState upperExisting = level.getBlockState(upperPos.getX(), upperPos.getY(), upperPos.getZ());
        if (!registry.getComponents(upperExisting.getType()).get(BlockComponents.REPLACEABLE).get()) {
            return false;
        }

        Direction playerFacing = player.getHorizontalDirection();
        CardinalDirection doorDirection = playerFacing.rotateClockwise().getCardinalDirection();

        boolean hingeRight = resolveHinge(level, pos, playerFacing);
        BlockState lowerState = blockState
                .withTrait(BlockTraits.CARDINAL_DIRECTION, doorDirection)
                .withTrait(BlockTraits.IS_DOOR_HINGE, hingeRight)
                .withTrait(BlockTraits.IS_OPEN, false);

        BlockState upperState = lowerState.withTrait(BlockTraits.IS_UPPER_BLOCK, true);
        if (!level.setBlockState(pos, lowerState, true, false)) {
            return false;
        }

        level.setBlockState(upperPos, upperState, true, true);
        return true;
    }

    private boolean resolveHinge(Level level, Vector3i pos, Direction playerFacing) {
        Direction left = playerFacing.rotateCounterClockwise();
        Direction right = playerFacing.rotateClockwise();

        BlockState leftState = level.getBlockState(left.relative(pos));
        BlockState rightState = level.getBlockState(right.relative(pos));

        boolean leftIsDoorLower = isDoorLowerHalf(leftState);
        boolean rightIsDoorLower = isDoorLowerHalf(rightState);

        if (leftIsDoorLower && !rightIsDoorLower) {
            return true;
        }

        if (rightIsDoorLower && !leftIsDoorLower) {
            return false;
        }

        boolean rightIsSolid = registry.getComponents(rightState.getType()).get(BlockComponents.SOLID).get();
        boolean leftIsSolid = registry.getComponents(leftState.getType()).get(BlockComponents.SOLID).get();

        return rightIsSolid && !leftIsSolid;
    }

    private boolean isDoorLowerHalf(BlockState state) {
        if (!state.getTraits().containsKey(BlockTraits.IS_UPPER_BLOCK)) {
            return false;
        }

        if (!state.getTraits().containsKey(BlockTraits.IS_OPEN)) {
            return false;
        }

        return !state.ensureTrait(BlockTraits.IS_UPPER_BLOCK);
    }
}
