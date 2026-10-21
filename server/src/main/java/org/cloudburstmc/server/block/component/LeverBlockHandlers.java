package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.BlockDestroyHandler;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.RedstoneSignalHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.LeverDirection;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.level.CloudLevel;

@UtilityClass
public class LeverBlockHandlers {

    public static final RedstoneSignalHandler GET_REDSTONE_SIGNAL = (block, face) ->
            block.getState().ensureTrait(BlockTraits.IS_OPEN) ? 15 : 0;

    public static final RedstoneSignalHandler GET_DIRECT_REDSTONE_SIGNAL = (block, face) ->
            block.getState().ensureTrait(BlockTraits.IS_OPEN) && face == block.getState().ensureTrait(BlockTraits.LEVER_DIRECTION).getDirection().getOpposite() ? 15 : 0;

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        BlockState state = block.getState();
        boolean nowOn = !state.ensureTrait(BlockTraits.IS_OPEN);

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();

        level.setBlockState(pos, state.withTrait(BlockTraits.IS_OPEN, nowOn), false, true);

        int soundData = BlockPalette.INSTANCE.getDefinition(state).getRuntimeId();
        level.addLevelSoundEvent(pos, nowOn ? SoundEvent.BLOCK_CLICK : SoundEvent.BLOCK_CLICK_FAIL, soundData);

        level.updateAround(pos);
        LeverDirection leverDirection = state.ensureTrait(BlockTraits.LEVER_DIRECTION);
        Vector3i attachedPos = PlacementSupport.supportPosition(pos, leverDirection.getDirection());
        level.updateAround(attachedPos);

        return true;
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        LeverDirection leverDirection = state.ensureTrait(BlockTraits.LEVER_DIRECTION);
        Vector3i pos = block.getPosition();
        Direction supportFace = leverDirection.getDirection();
        Vector3i supportPos = PlacementSupport.supportPosition(pos, supportFace);

        if (!neighbor.getPosition().equals(supportPos)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        if (PlacementSupport.hasFullFaceSupport(level, pos, supportFace)) {
            return;
        }

        level.breakBlock(pos, null, null, true);
    };

    public static final BlockDestroyHandler ON_DESTROY = (block, cause) -> {
        BlockState state = block.getState();
        block.set(BlockStates.AIR);
        if (!state.ensureTrait(BlockTraits.IS_OPEN)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        LeverDirection leverDirection = state.ensureTrait(BlockTraits.LEVER_DIRECTION);
        Vector3i attachedPos = PlacementSupport.supportPosition(pos, leverDirection.getDirection());

        level.updateAround(pos);
        level.updateAround(attachedPos);
    };
}
