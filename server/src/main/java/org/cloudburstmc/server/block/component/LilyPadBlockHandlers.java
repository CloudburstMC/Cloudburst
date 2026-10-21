package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.EntityInsideBlockHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.entity.vehicle.Boat;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;

@UtilityClass
public class LilyPadBlockHandlers {

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        if (!block.getLiquid().isEmpty()) {
            return false;
        }

        Block below = block.getRelative(0, -1, 0);
        LiquidState liquid = below.getLiquid();
        return below.getState().is(BlockTags.SUPPORTS_LILY_PAD) || liquid.isSource() && liquid.getType().isSameFamily(LiquidTypes.WATER);
    };

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        if (!(entity instanceof Boat)) {
            return;
        }

        EntityChangeBlockEvent event = new EntityChangeBlockEvent(entity, block, BlockStates.AIR);
        block.getLevel().getServer().getEventManager().fire(event);
        if (!event.isCancelled() && block.getLevel().getBlockState(block.getPosition()) == block.getState()) {
            block.getLevel().breakBlock(block.getPosition(), null, null, true);
        }
    };
}
