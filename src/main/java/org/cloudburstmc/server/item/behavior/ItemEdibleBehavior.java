package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.event.player.PlayerItemConsumeEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.food.Food;
import org.cloudburstmc.server.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class ItemEdibleBehavior extends CloudItemBehavior {

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        if (player.getFoodData().getLevel() < player.getFoodData().getMaxLevel() || player.isCreative()) {
            return true;
        }
        player.getFoodData().sendFoodLevel();
        return false;
    }

    @Override
    public ItemStack onUse(ItemStack item, int ticksUsed, Player player) {
        PlayerItemConsumeEvent consumeEvent = new PlayerItemConsumeEvent(player, item);

        player.getServer().getEventManager().fire(consumeEvent);
        if (consumeEvent.isCancelled()) {
            player.getInventory().sendContents(player);
            return null;
        }

        Food food = Food.getByRelative(item);
        if (player.isSurvival() && food != null && food.eatenBy(player)) {
            return item.decrementAmount();
        }
        return null;
    }
}
