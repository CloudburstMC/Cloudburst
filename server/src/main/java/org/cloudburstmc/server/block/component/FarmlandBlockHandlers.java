package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.component.FallOnBlockHandler;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.event.entity.EntityInteractEvent;
import org.cloudburstmc.api.event.player.PlayerInteractEvent;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.block.util.BlockShapeTransitions;
import org.cloudburstmc.server.level.CloudLevel;

@UtilityClass
public class FarmlandBlockHandlers {

    public static final FallOnBlockHandler FALL_ON = (block, entity, fallDistance) -> {
        DefaultBlockHandlers.ON_FALL_ON.execute(block, entity, fallDistance);
        CloudLevel level = (CloudLevel) block.getLevel();
        if (!(entity instanceof Living)
                || entity.getWidth() * entity.getWidth() * entity.getHeight() <= 0.512f
                || level.getRandom().nextFloat() >= fallDistance - 0.5f) {
            return;
        }

        Event interaction;
        if (entity instanceof Player player) {
            if (player.isSpectator() || !player.isOp() && level.isInSpawnRadius(block.getPosition())) {
                return;
            }

            interaction = new PlayerInteractEvent(player, null, block, null, PlayerInteractEvent.Action.PHYSICAL);
        } else {
            if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
                return;
            }

            interaction = new EntityInteractEvent(entity, block);
        }

        level.getServer().getEventManager().fire(interaction);
        if (interaction.isCancelled()) {
            return;
        }

        EntityChangeBlockEvent change = new EntityChangeBlockEvent(entity, block, BlockStates.DIRT);
        level.getServer().getEventManager().fire(change);
        if (change.isCancelled() || !level.getBlockState(block.getPosition()).equals(block.getState())) {
            return;
        }

        BlockShapeTransitions.pushEntitiesUp(level, block.getPosition(), block.getState(), change.getBlockState());
        level.setBlockState(block.getPosition(), change.getBlockState());
    };
}
