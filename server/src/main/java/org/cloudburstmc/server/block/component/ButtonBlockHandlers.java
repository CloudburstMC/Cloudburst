package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.RedstoneSignalHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.level.CloudLevel;

@UtilityClass
public class ButtonBlockHandlers {

    public static final int WOODEN_PRESS_TICKS = 30;
    public static final int STONE_PRESS_TICKS = 20;

    public static final RedstoneSignalHandler GET_REDSTONE_SIGNAL = (block, face) ->
            block.getState().ensureTrait(BlockTraits.IS_BUTTON_PRESSED) ? 15 : 0;

    public static final RedstoneSignalHandler GET_DIRECT_REDSTONE_SIGNAL = (block, face) ->
            block.getState().ensureTrait(BlockTraits.IS_BUTTON_PRESSED)
                    && face == block.getState().ensureTrait(BlockTraits.FACING_DIRECTION).getOpposite() ? 15 : 0;

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        BlockState state = block.getState();
        if (!state.ensureTrait(BlockTraits.IS_BUTTON_PRESSED)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();

        BlockState released = state.withTrait(BlockTraits.IS_BUTTON_PRESSED, false);
        level.setBlockState(pos, released, false, true);
        level.updateAround(PlacementSupport.supportPosition(pos, state.ensureTrait(BlockTraits.FACING_DIRECTION)));

        int soundData = BlockPalette.INSTANCE.getDefinition(released).getRuntimeId();
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

        level.breakBlock(pos, null, null, true);
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
        level.updateAround(PlacementSupport.supportPosition(pos, state.ensureTrait(BlockTraits.FACING_DIRECTION)));

        int soundData = BlockPalette.INSTANCE.getDefinition(pressed).getRuntimeId();
        level.addLevelSoundEvent(pos, SoundEvent.BUTTON_CLICK_ON, soundData);

        int pressDurationTicks = block.requireComponent(BlockComponents.BUTTON_PRESS_DURATION_TICKS);
        level.scheduleUpdate(level.getBlock(pos), pressDurationTicks);

        return true;
    };
}
