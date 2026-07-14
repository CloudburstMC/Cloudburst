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
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public class DoorPlaceHandler implements PlaceBlockHandler {

    private final CloudBlockRegistry registry;

    public DoorPlaceHandler(CloudBlockRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        if (player == null) {
            return false;
        }

        if (face != Direction.UP) {
            return false;
        }

        Level level = player.getLevel();
        if (!PlacementSupport.hasFloorSupport(level, blockPosition)) {
            return false;
        }

        Vector3i upperPosition = blockPosition.add(0, 1, 0);
        BlockState upperExisting = level.getBlockState(upperPosition.getX(), upperPosition.getY(), upperPosition.getZ());
        if (!registry.getComponents(upperExisting.getType()).get(BlockComponents.REPLACEABLE).get()) {
            return false;
        }

        Direction playerFacing = player.getHorizontalDirection();
        CardinalDirection doorDirection = playerFacing.rotateClockwise().getCardinalDirection();

        boolean hingeRight = resolveHinge(level, blockPosition, playerFacing, clickPosition);
        BlockState lowerState = blockState
                .withTrait(BlockTraits.CARDINAL_DIRECTION, doorDirection)
                .withTrait(BlockTraits.IS_DOOR_HINGE, hingeRight)
                .withTrait(BlockTraits.IS_OPEN, false);

        BlockState upperState = lowerState.withTrait(BlockTraits.IS_UPPER_BLOCK, true);
        if (!level.setBlockState(blockPosition, lowerState, true, false)) {
            return false;
        }

        level.setBlockState(upperPosition, upperState, true, true);
        return true;
    }

    private boolean resolveHinge(Level level, Vector3i blockPosition, Direction playerFacing, Vector3f clickPosition) {
        Direction left = playerFacing.rotateCounterClockwise();
        Direction right = playerFacing.rotateClockwise();

        Vector3i leftPosition = left.relative(blockPosition);
        Vector3i rightPosition = right.relative(blockPosition);
        Vector3i upperPosition = blockPosition.add(0, 1, 0);
        Vector3i leftUpperPosition = left.relative(upperPosition);
        Vector3i rightUpperPosition = right.relative(upperPosition);

        BlockState leftState = level.getBlockState(leftPosition);
        BlockState rightState = level.getBlockState(rightPosition);

        boolean leftIsDoorLower = isDoorLowerHalf(leftState);
        boolean rightIsDoorLower = isDoorLowerHalf(rightState);
        int obstructionBalance = (BlockSupport.isCollisionShapeFullBlock(level, leftPosition) ? -1 : 0)
                + (BlockSupport.isCollisionShapeFullBlock(level, leftUpperPosition) ? -1 : 0)
                + (BlockSupport.isCollisionShapeFullBlock(level, rightPosition) ? 1 : 0)
                + (BlockSupport.isCollisionShapeFullBlock(level, rightUpperPosition) ? 1 : 0);

        if ((leftIsDoorLower && !rightIsDoorLower) || obstructionBalance > 0) {
            return true;
        }

        if ((rightIsDoorLower && !leftIsDoorLower) || obstructionBalance < 0) {
            return false;
        }

        int stepX = playerFacing.getStepX();
        int stepZ = playerFacing.getStepZ();
        float clickX = clickPosition.getX();
        float clickZ = clickPosition.getZ();

        return (stepX < 0 && clickZ < 0.5f)
                || (stepX > 0 && clickZ > 0.5f)
                || (stepZ < 0 && clickX > 0.5f)
                || (stepZ > 0 && clickX < 0.5f);
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
