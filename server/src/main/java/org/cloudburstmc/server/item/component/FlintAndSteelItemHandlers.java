package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.block.BlockIgniteEvent;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.NetherPortals;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class FlintAndSteelItemHandlers {

    public static final UseOnHandler USE_ON = (item, entity, blockPosition, face, clickPosition) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        CloudLevel level = player.getLevel();
        Vector3i targetPos = face.relative(blockPosition);
        Block targetBlock = level.getBlock(targetPos.getX(), targetPos.getY(), targetPos.getZ());

        if (targetBlock.getState().getType() != BlockTypes.AIR) {
            return item;
        }

        BlockIgniteEvent event = new BlockIgniteEvent(targetBlock, null, player, BlockIgniteEvent.BlockIgniteCause.FLINT_AND_STEEL);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return item;
        }

        level.setBlockState(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0, BlockStates.FIRE, false, true);
        level.addLevelSoundEvent(targetPos, SoundEvent.IGNITE);
        NetherPortals.detect(level, targetPos).ifPresent(frame -> frame.fill(level));

        if (player.isCreative()) {
            return item;
        }

        return DefaultItemHandlers.ON_DAMAGE.execute(item, 1, player);
    };
}
