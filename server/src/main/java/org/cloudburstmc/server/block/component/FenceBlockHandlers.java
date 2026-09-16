package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.trait.BooleanBlockTrait;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;

@UtilityClass
public class FenceBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, clickPosition) -> resolveConnections(block.getLevel(), block.getPosition(), state);

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        Direction direction = BlockConnectionSupport.horizontalDirectionTo(block.getPosition(), neighbor.getPosition());
        if (direction == null) {
            return;
        }

        BlockState state = block.getState();
        BlockState updatedState = state.withTrait(connectionTrait(direction), connectsTo(block.getLevel(), neighbor.getPosition(), state, direction));
        if (updatedState != state) {
            block.set(updatedState, false, true);
        }
    };

    public static BlockState resolveConnections(Level level, Vector3i position, BlockState state) {
        BlockState resolved = state;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vector3i neighborPosition = direction.relative(position);
            resolved = resolved.withTrait(connectionTrait(direction), connectsTo(level, neighborPosition, state, direction));
        }

        return resolved;
    }

    public static boolean connectsTo(BlockState fence, BlockState neighbor, boolean faceSturdy, Direction connectionDirection) {
        boolean sameFenceFamily = neighbor.is(BlockTags.FENCE) && fence.is(BlockTags.WOODEN_FENCE) == neighbor.is(BlockTags.WOODEN_FENCE);
        return sameFenceFamily || BlockConnectionSupport.isAlignedFenceGate(neighbor, connectionDirection) || faceSturdy && BlockConnectionSupport.allowsSturdyFaceConnection(neighbor);
    }

    private static boolean connectsTo(Level level, Vector3i neighborPosition, BlockState fence, Direction connectionDirection) {
        BlockState neighbor = level.getBlockState(neighborPosition);
        boolean faceSturdy = BlockSupport.isFaceSturdy(level, neighborPosition, connectionDirection.getOpposite());
        return connectsTo(fence, neighbor, faceSturdy, connectionDirection);
    }

    private static BooleanBlockTrait connectionTrait(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockTraits.CONNECTION_NORTH;
            case EAST -> BlockTraits.CONNECTION_EAST;
            case SOUTH -> BlockTraits.CONNECTION_SOUTH;
            case WEST -> BlockTraits.CONNECTION_WEST;
            default -> throw new IllegalArgumentException("Fence connections require a horizontal direction: " + direction);
        };
    }
}
