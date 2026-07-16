package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlayerBlockHandler;
import org.cloudburstmc.api.block.component.ResourceBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.block.component.UseCheckHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class DoorBlockHandlers {

    public static final UseCheckHandler CAN_BE_USED = (block, player) -> block.getState().getType() != BlockTypes.IRON_DOOR;

    public static final ResourceBlockHandler GET_RESOURCE = (block, random, bonusLevel) ->
            block.getState().ensureTrait(BlockTraits.IS_UPPER_BLOCK)
                    ? ItemStack.EMPTY
                    : DefaultBlockHandlers.GET_RESOURCE.execute(block, random, bonusLevel);

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

        if (!isUpperBlock) {
            ItemStack drop = block.getComponent(BlockComponents.GET_RESOURCE)
                    .execute(block, ThreadLocalRandom.current(), 0);
            if (!drop.isEmpty()) {
                level.dropItem(pos.toFloat().add(0.5f, 0.5f, 0.5f), drop);
            }
        }

        level.addParticle(new DestroyBlockParticle(pos.toFloat().add(0.5f, 0.5f, 0.5f), state));
        block.set(BlockStates.AIR, false, true);
    };

    public static final PlayerBlockHandler ON_DESTROY = (block, player) -> {
        BlockState state = block.getState();
        boolean isUpperBlock = state.ensureTrait(BlockTraits.IS_UPPER_BLOCK);
        Vector3i pos = block.getPosition();
        Vector3i partnerPos = isUpperBlock ? pos.sub(0, 1, 0) : pos.add(0, 1, 0);

        CloudLevel level = (CloudLevel) block.getLevel();
        BlockState partnerState = level.getBlockState(partnerPos.getX(), partnerPos.getY(), partnerPos.getZ());

        level.setBlockState(pos, BlockStates.AIR, false, false);

        if (partnerState.getType() == state.getType()) {
            level.addParticle(new DestroyBlockParticle(partnerPos.toFloat().add(0.5f, 0.5f, 0.5f), partnerState));
            level.setBlockState(partnerPos, BlockStates.AIR, false, false);
            level.updateAround(partnerPos);
        }

        level.updateAround(pos);
    };
}
