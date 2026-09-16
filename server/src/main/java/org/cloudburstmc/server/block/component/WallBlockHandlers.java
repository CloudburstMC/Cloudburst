package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.trait.EnumBlockTrait;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.data.WallConnectionType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

@UtilityClass
public class WallBlockHandlers {

    private static final float PIXEL = 1f / 16f;
    private static final VoxelShape POST_TEST_SHAPE = CloudVoxelShapes.box(7 * PIXEL, 0, 7 * PIXEL, 9 * PIXEL, 1, 9 * PIXEL);

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, clickPosition) -> resolveShape(block.getLevel(), block.getPosition(), state);

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        Vector3i position = block.getPosition();
        Vector3i neighborPosition = neighbor.getPosition();
        if (neighborPosition.equals(Direction.DOWN.relative(position))) {
            return;
        }

        if (!neighborPosition.equals(Direction.UP.relative(position)) && BlockConnectionSupport.horizontalDirectionTo(position, neighborPosition) == null) {
            return;
        }

        BlockState state = block.getState();
        BlockState updatedState = resolveShape(block.getLevel(), position, state);
        if (updatedState != state) {
            block.set(updatedState, false, true);
        }
    };

    public static BlockState resolveShape(Level level, Vector3i position, BlockState state) {
        Vector3i abovePosition = Direction.UP.relative(position);
        BlockState above = level.getBlockState(abovePosition);
        VoxelShape aboveFace = level.getServer().getBlockRegistry()
                .requireComponent(above.getType(), BlockComponents.GET_COLLISION_SHAPE)
                .execute(above, BlockShapeContext.at(level, abovePosition), CollisionContext.empty())
                .getFaceShape(Direction.DOWN);

        BlockState resolved = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vector3i neighborPosition = direction.relative(position);
            boolean connected = connectsTo(level, neighborPosition, direction);
            WallConnectionType connection = connected && aboveFace.covers(sideTestShape(direction))
                    ? WallConnectionType.TALL
                    : connected ? WallConnectionType.SHORT : WallConnectionType.NONE;
            resolved = resolved.withTrait(connectionTrait(direction), connection);
        }

        return resolved.withTrait(BlockTraits.HAS_POST, shouldRaisePost(resolved, above, aboveFace));
    }

    public static boolean connectsTo(BlockState neighbor, boolean faceSturdy, Direction connectionDirection) {
        return neighbor.is(BlockTags.WALLS)
                || isBarOrPane(neighbor)
                || BlockConnectionSupport.isAlignedFenceGate(neighbor, connectionDirection)
                || faceSturdy && BlockConnectionSupport.allowsSturdyFaceConnection(neighbor);
    }

    private static boolean connectsTo(Level level, Vector3i neighborPosition, Direction connectionDirection) {
        BlockState neighbor = level.getBlockState(neighborPosition);
        boolean faceSturdy = BlockSupport.isFaceSturdy(level, neighborPosition, connectionDirection.getOpposite());
        return connectsTo(neighbor, faceSturdy, connectionDirection);
    }

    private static boolean shouldRaisePost(BlockState state, BlockState above, VoxelShape aboveFace) {
        if (above.is(BlockTags.WALLS) && above.ensureTrait(BlockTraits.HAS_POST)) {
            return true;
        }

        WallConnectionType north = state.ensureTrait(BlockTraits.WALL_CONNECTION_NORTH);
        WallConnectionType east = state.ensureTrait(BlockTraits.WALL_CONNECTION_EAST);
        WallConnectionType south = state.ensureTrait(BlockTraits.WALL_CONNECTION_SOUTH);
        WallConnectionType west = state.ensureTrait(BlockTraits.WALL_CONNECTION_WEST);

        boolean northConnected = north != WallConnectionType.NONE;
        boolean eastConnected = east != WallConnectionType.NONE;
        boolean southConnected = south != WallConnectionType.NONE;
        boolean westConnected = west != WallConnectionType.NONE;

        boolean cornerOrIsolated = (!northConnected && !eastConnected && !southConnected && !westConnected)
                || northConnected != southConnected
                || eastConnected != westConnected;
        if (cornerOrIsolated) {
            return true;
        }

        boolean opposingTallSides = (north == WallConnectionType.TALL && south == WallConnectionType.TALL)
                || (east == WallConnectionType.TALL && west == WallConnectionType.TALL);
        return !opposingTallSides
                && (above.is(BlockTags.WALL_POST_OVERRIDE) || aboveFace.covers(POST_TEST_SHAPE));
    }

    private static boolean isBarOrPane(BlockState state) {
        return !state.is(BlockTags.FENCE)
                && state.getType() != BlockTypes.TRIP_WIRE
                && state.getTraits().containsKey(BlockTraits.CONNECTION_NORTH)
                && state.getTraits().containsKey(BlockTraits.CONNECTION_EAST)
                && state.getTraits().containsKey(BlockTraits.CONNECTION_SOUTH)
                && state.getTraits().containsKey(BlockTraits.CONNECTION_WEST);
    }

    private static EnumBlockTrait<WallConnectionType> connectionTrait(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockTraits.WALL_CONNECTION_NORTH;
            case EAST -> BlockTraits.WALL_CONNECTION_EAST;
            case SOUTH -> BlockTraits.WALL_CONNECTION_SOUTH;
            case WEST -> BlockTraits.WALL_CONNECTION_WEST;
            default -> throw new IllegalArgumentException("Wall connections require a horizontal direction: " + direction);
        };
    }

    private static VoxelShape sideTestShape(Direction direction) {
        return switch (direction) {
            case NORTH -> CloudVoxelShapes.box(7 * PIXEL, 0, 0, 9 * PIXEL, 1, 9 * PIXEL);
            case EAST -> CloudVoxelShapes.box(7 * PIXEL, 0, 7 * PIXEL, 1, 1, 9 * PIXEL);
            case SOUTH -> CloudVoxelShapes.box(7 * PIXEL, 0, 7 * PIXEL, 9 * PIXEL, 1, 1);
            case WEST -> CloudVoxelShapes.box(0, 0, 7 * PIXEL, 9 * PIXEL, 1, 9 * PIXEL);
            default -> throw new IllegalArgumentException("Wall connections require a horizontal direction: " + direction);
        };
    }
}
