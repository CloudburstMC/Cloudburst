package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockChange;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.FertilizationResult;
import org.cloudburstmc.api.event.block.BlockFertilizeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.particle.BoneMealParticle;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.concurrent.ThreadLocalRandom;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BoneMealItemHandlers {

    public static final UseOnHandler USE_ON = (item, entity, position, face, click) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        CloudLevel level = player.getLevel();
        Block block = level.getBlock(position);
        FertilizationResult result = block.requireComponent(BlockComponents.FERTILIZE).execute(block, player, ThreadLocalRandom.current());
        if (!result.succeeded()) {
            return item;
        }

        BlockFertilizeEvent event = new BlockFertilizeEvent(block, player, result.changes());
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return item;
        }

        level.batchBlockUpdates(() -> {
            for (BlockChange change : event.getBlocks()) {
                change.block().set(change.newState());
            }
        });

        for (ItemStack drop : result.drops()) {
            level.dropItem(block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), drop);
        }

        level.addParticle(new BoneMealParticle(position));
        level.addSound(position, Sound.ITEM_BONE_MEAL_USE);
        return player.isCreative() ? item : item.decreaseCount();
    };
}
