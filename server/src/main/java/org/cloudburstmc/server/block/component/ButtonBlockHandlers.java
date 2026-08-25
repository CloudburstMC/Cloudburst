package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

@UtilityClass
public class ButtonBlockHandlers {

    public static final int WOODEN_PRESS_TICKS = 30;
    public static final int STONE_PRESS_TICKS = 20;

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        BlockState state = block.getState();
        if (!state.ensureTrait(BlockTraits.IS_BUTTON_PRESSED)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();

        BlockState released = state.withTrait(BlockTraits.IS_BUTTON_PRESSED, false);
        level.setBlockState(pos, released, false, true);

        int soundData = CloudBlockRegistry.REGISTRY.getRuntimeId(released);
        level.addLevelSoundEvent(pos, SoundEvent.BUTTON_CLICK_OFF, soundData);
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        Direction facing = state.ensureTrait(BlockTraits.FACING_DIRECTION);
        Vector3i pos = block.getPosition();

        Vector3i attachedPos = PlacementSupport.supportPosition(pos, facing);
        if (!neighbor.getPosition().equals(attachedPos)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        if (PlacementSupport.hasFullFaceSupport(level, pos, facing)) {
            return;
        }

        DefaultBlockHandlers.dropLoot(block);

        level.addParticle(new DestroyBlockParticle(pos.toFloat().add(0.5f, 0.5f, 0.5f), state));
        block.set(BlockStates.AIR, false, true);
    };

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        BlockState state = block.getState();
        if (state.ensureTrait(BlockTraits.IS_BUTTON_PRESSED)) {
            return true;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();

        BlockState pressed = state.withTrait(BlockTraits.IS_BUTTON_PRESSED, true);
        level.setBlockState(pos, pressed, false, true);

        int soundData = CloudBlockRegistry.REGISTRY.getRuntimeId(pressed);
        level.addLevelSoundEvent(pos, SoundEvent.BUTTON_CLICK_ON, soundData);

        int pressDurationTicks = block.requireComponent(BlockComponents.BUTTON_PRESS_DURATION_TICKS);
        level.scheduleUpdate(level.getBlock(pos), pressDurationTicks);

        return true;
    };

}
