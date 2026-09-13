package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.entity.EntityBlockChangeEvent;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.util.BlockShapeTransitions;
import org.cloudburstmc.server.block.util.EndPortalFrame;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

@UtilityClass
public class EnderEyeItemHandlers {

    public static final UseOnHandler USE_ON = (item, entity, position, face, click) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        CloudLevel level = player.getLevel();
        Block frame = level.getBlock(position);
        BlockState state = frame.getState();
        if (state.getType() != BlockTypes.END_PORTAL_FRAME || state.ensureTrait(BlockTraits.HAS_END_PORTAL_EYE)) {
            return item;
        }

        BlockState filledState = state.withTrait(BlockTraits.HAS_END_PORTAL_EYE, true);
        EntityBlockChangeEvent event = new EntityBlockChangeEvent(player, frame, filledState);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return item;
        }

        BlockShapeTransitions.pushEntitiesUp(level, position, state, filledState);
        frame.set(filledState);
        level.updateComparatorOutputLevel(position);
        level.addLevelSoundEvent(position, SoundEvent.BLOCK_END_PORTAL_FRAME_FILL);

        EndPortalFrame portal = EndPortalFrame.find(level, position);
        if (portal != null) {
            portal.fill(level);
            level.addLevelSoundEvent(portal.center(), SoundEvent.BLOCK_END_PORTAL_SPAWN);
        }

        return player.isCreative() ? item : item.decreaseCount();
    };
}
