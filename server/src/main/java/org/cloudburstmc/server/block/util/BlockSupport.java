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
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

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
        return CloudVoxelShapes.isFullBlock(state.getCollisionShape());
    }

    public static boolean blocksMotion(Level level, Vector3i pos) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return blocksMotion(state);
    }

    public static boolean blocksMotion(BlockState state) {
        return !state.getCollisionShape().isEmpty();
    }

    public static boolean isSolid(BlockState state) {
        return state.isSolid();
    }

    public static boolean isViewBlocking(BlockState state) {
        return !state.is(BlockTags.TRANSPARENT) && blocksMotion(state) && isCollisionShapeFullBlock(state);
    }

    public static boolean isPassable(BlockState state) {
        return !blocksMotion(state);
    }

    public static boolean isFaceSturdy(Level level, Vector3i pos, Direction direction) {
        return isFaceSturdy(level, pos, direction, SupportType.FULL);
    }

    public static VoxelShape getBlockSupportShape(Level level, Vector3i pos) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        return getSupportShapeHandler(level.getServer().getBlockRegistry(), state)
                .execute(state, BlockShapeContext.at(level, pos));
    }

    public static VoxelShape getBlockSupportShape(BlockRegistry registry, BlockState state) {
        return getSupportShapeHandler(registry, state).execute(state, BlockShapeContext.empty());
    }

    public static boolean isFaceSturdy(Level level, Vector3i pos, Direction direction, SupportType supportType) {
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
        BlockSupportShapeHandler handler = getSupportShapeHandler(level.getServer().getBlockRegistry(), state);
        if (handler.contextRequirement() != ShapeContextRequirement.STATE_ONLY) {
            return hasRequiredSupport(handler.execute(state, BlockShapeContext.at(level, pos)), direction, supportType);
        }

        return getCachedFaceSupport(level.getServer().getBlockRegistry(), state, direction, supportType);
    }

    public static boolean isFaceSturdy(BlockRegistry registry, BlockState state, Direction direction, SupportType supportType) {
        BlockSupportShapeHandler handler = getSupportShapeHandler(registry, state);
        if (handler.contextRequirement() != ShapeContextRequirement.STATE_ONLY) {
            return hasRequiredSupport(handler.execute(state, BlockShapeContext.empty()), direction, supportType);
        }

        return getCachedFaceSupport(registry, state, direction, supportType);
    }

    private static BlockSupportShapeHandler getSupportShapeHandler(BlockRegistry registry, BlockState state) {
        return checkNotNull(registry.getComponent(state.getType(), BlockComponents.GET_BLOCK_SUPPORT_SHAPE),
                "Block support shape component is not registered for %s", state.getType());
    }

    private static boolean getCachedFaceSupport(BlockRegistry registry, BlockState state, Direction direction,
                                                SupportType supportType) {
        BlockSupportShapeHandler handler = getSupportShapeHandler(registry, state);
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
