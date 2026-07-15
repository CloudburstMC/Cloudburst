package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.SupportType;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.BlockSupportShapeHandler;
import org.cloudburstmc.api.block.component.ShapeContextRequirement;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@UtilityClass
public class BlockSupport {

    private static final float PIXEL = 1f / 16f;
    private static final VoxelShape FULL_SUPPORT_SHAPE = CloudVoxelShapes.block();
    private static final VoxelShape CENTER_SUPPORT_SHAPE =
            CloudVoxelShapes.box(7 * PIXEL, 0, 7 * PIXEL, 9 * PIXEL, 10 * PIXEL, 9 * PIXEL);
    private static final VoxelShape RIGID_SUPPORT_SHAPE = CloudVoxelShapes.fromBoxes(
            0, 0, 0, 1, 1, 2 * PIXEL,
            0, 0, 14 * PIXEL, 1, 1, 1,
            0, 0, 2 * PIXEL, 2 * PIXEL, 1, 14 * PIXEL,
            14 * PIXEL, 0, 2 * PIXEL, 1, 1, 14 * PIXEL
    );
    private static final int SUPPORT_TYPE_COUNT = SupportType.values().length;
    private static final ConcurrentMap<BlockState, FaceSupportCache> FACE_SUPPORT_CACHE = new ConcurrentHashMap<>();

    public static boolean canSupportCenter(Level level, Vector3i pos, Direction direction) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        if (direction == Direction.DOWN && state.is(BlockTags.UNSTABLE_BOTTOM_CENTER)) {
            return false;
        }
        return isFaceSturdy(level, pos, direction, SupportType.CENTER);
    }

    public static boolean canSupportRigidBlock(Level level, Vector3i pos) {
        return isFaceSturdy(level, pos, Direction.UP, SupportType.RIGID);
    }

    public static boolean isCollisionShapeFullBlock(Level level, Vector3i pos) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return isCollisionShapeFullBlock(state);
    }

    public static boolean isCollisionShapeFullBlock(BlockState state) {
        VoxelShape shape = CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.GET_COLLISION_SHAPE)
                .execute(state, BlockShapeContext.empty(), CollisionContext.empty());
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
                .execute(state, BlockShapeContext.empty(), CollisionContext.empty());
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
        return isFaceSturdy(level, pos, direction, SupportType.FULL);
    }

    public static VoxelShape getBlockSupportShape(Level level, Vector3i pos) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return getSupportShapeHandler(state).execute(state, BlockShapeContext.at(level, pos));
    }

    public static VoxelShape getBlockSupportShape(BlockState state) {
        return getSupportShapeHandler(state).execute(state, BlockShapeContext.empty());
    }

    public static boolean isFaceSturdy(Level level, Vector3i pos, Direction direction, SupportType supportType) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        BlockSupportShapeHandler handler = getSupportShapeHandler(state);
        if (handler.contextRequirement() != ShapeContextRequirement.STATE_ONLY) {
            return hasRequiredSupport(handler.execute(state, BlockShapeContext.at(level, pos)), direction, supportType);
        }

        return getCachedFaceSupport(state, direction, supportType);
    }

    public static boolean isFaceSturdy(BlockState state, Direction direction, SupportType supportType) {
        BlockSupportShapeHandler handler = getSupportShapeHandler(state);
        if (handler.contextRequirement() != ShapeContextRequirement.STATE_ONLY) {
            return hasRequiredSupport(handler.execute(state, BlockShapeContext.empty()), direction, supportType);
        }

        return getCachedFaceSupport(state, direction, supportType);
    }

    private static BlockSupportShapeHandler getSupportShapeHandler(BlockState state) {
        return CloudBlockRegistry.REGISTRY.getComponent(state.getType(), BlockComponents.GET_BLOCK_SUPPORT_SHAPE);
    }

    private static boolean getCachedFaceSupport(BlockState state, Direction direction, SupportType supportType) {
        BlockSupportShapeHandler handler = getSupportShapeHandler(state);
        FaceSupportCache cache = FACE_SUPPORT_CACHE.compute(state, (ignored, existing) ->
                existing != null && existing.handler() == handler
                        ? existing
                        : new FaceSupportCache(handler, computeSupportMask(state, handler)));
        return cache.supports(direction, supportType);
    }

    private static long computeSupportMask(BlockState state, BlockSupportShapeHandler handler) {
        VoxelShape shape = handler.execute(state, BlockShapeContext.empty());
        long supportMask = 0;
        for (Direction direction : Direction.values()) {
            for (SupportType supportType : SupportType.values()) {
                if (hasRequiredSupport(shape, direction, supportType)) {
                    supportMask |= supportBit(direction, supportType);
                }
            }
        }

        return supportMask;
    }

    private static long supportBit(Direction direction, SupportType supportType) {
        int index = direction.ordinal() * SUPPORT_TYPE_COUNT + supportType.ordinal();
        return 1L << index;
    }

    static boolean hasRequiredSupport(VoxelShape shape, Direction direction, SupportType supportType) {
        VoxelShape required = switch (supportType) {
            case FULL -> FULL_SUPPORT_SHAPE;
            case CENTER -> CENTER_SUPPORT_SHAPE;
            case RIGID -> RIGID_SUPPORT_SHAPE;
        };

        return shape.getFaceShape(direction).covers(required);
    }

    private record FaceSupportCache(BlockSupportShapeHandler handler, long supportMask) {
        boolean supports(Direction direction, SupportType supportType) {
            return (this.supportMask & supportBit(direction, supportType)) != 0;
        }
    }
}
