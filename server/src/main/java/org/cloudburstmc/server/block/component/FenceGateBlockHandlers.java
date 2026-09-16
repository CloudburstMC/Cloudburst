package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

@UtilityClass
public class FenceGateBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, clickPosition) -> {
        if (player == null) {
            return state;
        }

        Direction facing = player.getHorizontalDirection();
        return state
                .withTrait(BlockTraits.CARDINAL_DIRECTION, facing.getCardinalDirection())
                .withTrait(BlockTraits.IS_IN_WALL, shouldBeLowered(block.getLevel(), block.getPosition(), facing));
    };

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        BlockState state = block.getState();
        boolean nowOpen = !state.ensureTrait(BlockTraits.IS_OPEN);

        if (nowOpen && player != null) {
            CardinalDirection currentCardinal = state.ensureTrait(BlockTraits.CARDINAL_DIRECTION);
            Direction gateFacing = currentCardinal.toDirection();
            if (player.getHorizontalDirection() == gateFacing.getOpposite()) {
                state = state.withTrait(BlockTraits.CARDINAL_DIRECTION, gateFacing.getOpposite().getCardinalDirection());
            }
        }

        state = state.withTrait(BlockTraits.IS_OPEN, nowOpen);

        CloudLevel level = (CloudLevel) block.getLevel();
        level.setBlockState(block.getPosition(), state, false, true);

        int soundData = CloudBlockRegistry.REGISTRY.getRuntimeId(state);
        level.addLevelSoundEvent(block.getPosition(), nowOpen ? SoundEvent.FENCE_GATE_OPEN : SoundEvent.FENCE_GATE_CLOSE, soundData);

        return true;
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        CardinalDirection cardinal = state.ensureTrait(BlockTraits.CARDINAL_DIRECTION);
        Direction facing = cardinal.toDirection();
        Vector3i pos = block.getPosition();

        Direction left = facing.rotateCounterClockwise();
        Vector3i leftPos = left.relative(pos);
        Vector3i rightPos = left.getOpposite().relative(pos);

        Vector3i neighborPos = neighbor.getPosition();
        if (!neighborPos.equals(leftPos) && !neighborPos.equals(rightPos)) {
            return;
        }

        boolean shouldBeLowered = shouldBeLowered(block.getLevel(), pos, facing);
        if (shouldBeLowered != state.ensureTrait(BlockTraits.IS_IN_WALL)) {
            block.set(state.withTrait(BlockTraits.IS_IN_WALL, shouldBeLowered), false, true);
        }
    };

    private static boolean shouldBeLowered(Level level, Vector3i position, Direction facing) {
        Direction left = facing.rotateCounterClockwise();
        return level.getBlockState(left.relative(position)).is(BlockTags.WALLS) || level.getBlockState(left.getOpposite().relative(position)).is(BlockTags.WALLS);
    }
}
