package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.network.LevelEffectPacketFactory;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.List;

@UtilityClass
public class DoorBlockHandlers {

    public static final UseCheckHandler CAN_BE_USED = (block, player) -> block.getState().getType() != BlockTypes.IRON_DOOR;

    public static final BlockLootHandler GET_LOOT = (block, context) ->
            block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)
                    ? List.of()
                    : DefaultBlockHandlers.GET_LOOT.execute(block, context);

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        BlockState state = block.getState();
        boolean isUpperBlock = state.ensureTrait(BlockTraits.IS_UPPER_BLOCK);
        boolean nowOpen = !state.ensureTrait(BlockTraits.IS_OPEN);

        Vector3i thisPos = block.getPosition();
        Vector3i partnerPos = isUpperBlock ? thisPos.sub(0, 1, 0) : thisPos.add(0, 1, 0);

        CloudLevel level = (CloudLevel) block.getLevel();
        BlockState partnerState = level.getBlockState(partnerPos.getX(), partnerPos.getY(), partnerPos.getZ());

        level.setBlockState(thisPos, state.withTrait(BlockTraits.IS_OPEN, nowOpen), false, true);
        level.setBlockState(partnerPos, partnerState.withTrait(BlockTraits.IS_OPEN, nowOpen), false, true);

        int soundData = CloudBlockRegistry.REGISTRY.getRuntimeId(state);
        level.addLevelSoundEvent(thisPos, nowOpen ? SoundEvent.DOOR_OPEN : SoundEvent.DOOR_CLOSE, soundData);

        return true;
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        boolean isUpperBlock = state.ensureTrait(BlockTraits.IS_UPPER_BLOCK);
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();

        boolean shouldBreak = false;
        if (!isUpperBlock) {
            if (!PlacementSupport.hasFloorSupport(level, pos)) {
                shouldBreak = true;
            }
        }

        if (!shouldBreak) {
            Vector3i partnerPos = isUpperBlock ? pos.sub(0, 1, 0) : pos.add(0, 1, 0);
            BlockState partnerState = level.getBlockState(partnerPos.getX(), partnerPos.getY(), partnerPos.getZ());
            if (partnerState.getType() != state.getType()) {
                shouldBreak = true;
            }
        }

        if (!shouldBreak) {
            return;
        }

        level.breakBlock(pos, null, null, true);
    };

    public static final BlockDestroyHandler ON_DESTROY = (block, cause) -> {
        BlockState state = block.getState();
        boolean isUpperBlock = state.ensureTrait(BlockTraits.IS_UPPER_BLOCK);
        Vector3i pos = block.getPosition();
        Vector3i partnerPos = isUpperBlock ? pos.sub(0, 1, 0) : pos.add(0, 1, 0);

        CloudLevel level = (CloudLevel) block.getLevel();
        BlockState partnerState = level.getBlockState(partnerPos.getX(), partnerPos.getY(), partnerPos.getZ());

        level.setBlockState(pos, BlockStates.AIR, false, false);

        if (partnerState.getType() == state.getType()) {
            Vector3f position = partnerPos.toFloat().add(0.5f, 0.5f, 0.5f);
            level.addChunkPacket(position, LevelEffectPacketFactory.blockDestruction(position, partnerState, true));
            level.setBlockState(partnerPos, BlockStates.AIR, false, false);
            level.updateAround(partnerPos);
        }

        level.updateAround(pos);
    };
}
