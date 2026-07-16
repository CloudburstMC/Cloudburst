package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlayerBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.LeverDirection;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class LeverBlockHandlers {

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        BlockState state = block.getState();
        boolean nowOn = !state.ensureTrait(BlockTraits.IS_OPEN);

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();

        level.setBlockState(pos, state.withTrait(BlockTraits.IS_OPEN, nowOn), false, true);

        int soundData = CloudBlockRegistry.REGISTRY.getRuntimeId(state);
        level.addLevelSoundEvent(pos, nowOn ? SoundEvent.BLOCK_CLICK : SoundEvent.BLOCK_CLICK_FAIL, soundData);

        level.updateAround(pos);
        LeverDirection leverDirection = state.ensureTrait(BlockTraits.LEVER_DIRECTION);
        Vector3i attachedPos = leverDirection.getDirection().relative(pos);
        level.updateAround(attachedPos);

        return true;
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        LeverDirection leverDirection = state.ensureTrait(BlockTraits.LEVER_DIRECTION);
        Vector3i pos = block.getPosition();
        Direction supportFace = leverDirection.getDirection().getOpposite();
        Vector3i supportPos = PlacementSupport.supportPosition(pos, supportFace);

        if (!neighbor.getPosition().equals(supportPos)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        if (PlacementSupport.hasFullFaceSupport(level, pos, supportFace)) {
            return;
        }

        ItemStack drop = block.getComponent(BlockComponents.GET_RESOURCE)
                .execute(block, ThreadLocalRandom.current(), 0);
        if (!drop.isEmpty()) {
            level.dropItem(pos.toFloat().add(0.5f, 0.5f, 0.5f), drop);
        }

        level.addParticle(new DestroyBlockParticle(pos.toFloat().add(0.5f, 0.5f, 0.5f), state));
        block.set(BlockStates.AIR, false, true);
    };

    public static final PlayerBlockHandler ON_DESTROY = (block, player) -> {
        BlockState state = block.getState();
        if (!state.ensureTrait(BlockTraits.IS_OPEN)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        LeverDirection leverDirection = state.ensureTrait(BlockTraits.LEVER_DIRECTION);
        Vector3i attachedPos = leverDirection.getDirection().relative(pos);

        level.updateAround(pos);
        level.updateAround(attachedPos);
    };
}
