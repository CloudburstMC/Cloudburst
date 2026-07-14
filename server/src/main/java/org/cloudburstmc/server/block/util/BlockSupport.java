package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.SupportType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

@UtilityClass
public class BlockSupport {

    private static final float CENTER_MIN = 7f / 16f;
    private static final float CENTER_MAX = 9f / 16f;
    private static final float RIGID_MIN = 2f / 16f;
    private static final float RIGID_MAX = 14f / 16f;

    public static boolean canSupportCenter(Level level, Vector3i pos, Direction direction) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return isFaceSturdy(state, direction, SupportType.CENTER);
    }

    public static boolean canSupportRigidBlock(Level level, Vector3i pos) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return isFaceSturdy(state, Direction.UP, SupportType.RIGID);
    }

    public static boolean isCollisionShapeFullBlock(Level level, Vector3i pos) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return isCollisionShapeFullBlock(state);
    }

    public static boolean isCollisionShapeFullBlock(BlockState state) {
        VoxelShape shape = CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.GET_COLLISION_SHAPE)
                .execute(state, CollisionContext.empty());
        return CloudVoxelShapes.isFullBlock(shape);
    }

    public static boolean blocksMotion(Level level, Vector3i pos) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return blocksMotion(state);
    }

    public static boolean blocksMotion(BlockState state) {
        return CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.BLOCKS_MOTION).execute(state);
    }

    public static boolean defaultBlocksMotion(BlockState state) {
        VoxelShape shape = CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.GET_COLLISION_SHAPE)
                .execute(state, CollisionContext.empty());
        return !shape.isEmpty();
    }

    public static boolean canOcclude(BlockState state) {
        return CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.CAN_OCCLUDE).execute(state);
    }

    public static boolean isViewBlocking(BlockState state) {
        return CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.VIEW_BLOCKING).execute(state);
    }

    public static boolean isPassable(BlockState state) {
        return CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.PASSABLE).execute(state);
    }

    public static boolean isFaceSturdy(Level level, Vector3i pos, Direction direction) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return isFaceSturdy(state, direction, SupportType.FULL);
    }

    public static boolean isFaceSturdy(Level level, Vector3i pos, Direction direction, SupportType supportType) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return isFaceSturdy(state, direction, supportType);
    }

    public static boolean isFaceSturdy(BlockState state, Direction direction, SupportType supportType) {
        return CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.IS_FACE_STURDY)
                .execute(state, direction, supportType);
    }

    public static boolean defaultFaceSturdy(BlockState state, Direction direction, SupportType supportType) {
        VoxelShape shape = CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.GET_BLOCK_SUPPORT_SHAPE)
                .execute(state, CollisionContext.empty());
        if (shape.isEmpty()) {
            return false;
        }

        return switch (supportType) {
            case FULL -> hasSupportArea(shape, direction, 0f, 1f);
            case CENTER -> hasSupportArea(shape, direction, CENTER_MIN, CENTER_MAX);
            case RIGID -> hasSupportArea(shape, direction, RIGID_MIN, RIGID_MAX);
        };
    }

    private static boolean hasSupportArea(VoxelShape shape, Direction direction, float min, float max) {
        return shape.anyBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            if (!touchesFace(direction, minX, minY, minZ, maxX, maxY, maxZ)) {
                return false;
            }

            return switch (direction.getAxis()) {
                case X -> minY <= min && maxY >= max && minZ <= min && maxZ >= max;
                case Y -> minX <= min && maxX >= max && minZ <= min && maxZ >= max;
                case Z -> minX <= min && maxX >= max && minY <= min && maxY >= max;
            };
        });
    }

    private static boolean touchesFace(Direction direction, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return switch (direction) {
            case DOWN -> minY <= CloudVoxelShapes.EPSILON;
            case UP -> maxY >= 1f - CloudVoxelShapes.EPSILON;
            case NORTH -> minZ <= CloudVoxelShapes.EPSILON;
            case SOUTH -> maxZ >= 1f - CloudVoxelShapes.EPSILON;
            case WEST -> minX <= CloudVoxelShapes.EPSILON;
            case EAST -> maxX >= 1f - CloudVoxelShapes.EPSILON;
        };
    }
}
