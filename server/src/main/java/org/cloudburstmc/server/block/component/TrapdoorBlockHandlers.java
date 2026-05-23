package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.block.component.UseCheckHandler;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

@UtilityClass
public class TrapdoorBlockHandlers {

    public static final UseCheckHandler CAN_BE_USED = (block, player) -> block.getState().getType() != BlockTypes.IRON_TRAPDOOR;

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        BlockState state = block.getState();
        boolean nowOpen = !state.ensureTrait(BlockTraits.IS_OPEN);

        CloudLevel level = (CloudLevel) block.getLevel();
        level.setBlockState(block.getPosition(), state.withTrait(BlockTraits.IS_OPEN, nowOpen), false, true);

        int soundData = CloudBlockRegistry.REGISTRY.getRuntimeId(state);
        level.addLevelSoundEvent(block.getPosition(), nowOpen ? SoundEvent.TRAPDOOR_OPEN : SoundEvent.TRAPDOOR_CLOSE, soundData);

        return true;
    };
}
