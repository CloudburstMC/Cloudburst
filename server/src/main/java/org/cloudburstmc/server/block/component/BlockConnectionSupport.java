package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;

@UtilityClass
public class BlockConnectionSupport {

    public static @Nullable Direction horizontalDirectionTo(Vector3i position, Vector3i neighborPosition) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction.relative(position).equals(neighborPosition)) {
                return direction;
            }
        }

        return null;
    }

    public static boolean allowsSturdyFaceConnection(BlockState state) {
        return !state.is(BlockTags.LEAVES)
                && !state.is(BlockTags.SHULKER_BOXES)
                && state.getType() != BlockTypes.BARRIER
                && state.getType() != BlockTypes.CARVED_PUMPKIN
                && state.getType() != BlockTypes.LIT_PUMPKIN
                && state.getType() != BlockTypes.MELON_BLOCK
                && state.getType() != BlockTypes.PUMPKIN;
    }

    public static boolean isAlignedFenceGate(BlockState state, Direction connectionDirection) {
        return state.is(BlockTags.FENCE_GATE)
                && state.ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection().getAxis()
                == connectionDirection.rotateClockwise().getAxis();
    }
}
