package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.event.player.PlayerItemConsumeEvent;
import org.cloudburstmc.server.item.food.Food;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class ItemEdible extends Item {

    public ItemEdible(Identifier id) {
        super(id);
    }

    @Override
    public boolean onClickAir(Player player, Vector3f directionVector) {
        if (player.getFoodData().getLevel() < player.getFoodData().getMaxLevel() || player.isCreative()) {
            return true;
        }
        player.getFoodData().sendFoodLevel();
        return false;
    }

    @Override
    public boolean onUse(Player player, int ticksUsed) {
        PlayerItemConsumeEvent consumeEvent = new PlayerItemConsumeEvent(player, this);

        player.getServer().getEventManager().fire(consumeEvent);
        if (consumeEvent.isCancelled()) {
            player.getInventory().sendContents(player);
            return false;
        }

        Food food = Food.getByRelative(this);
        if (player.isSurvival() && food != null && food.eatenBy(player)) {
            decrementCount();
            player.getInventory().setItemInHand(this);
        }
        return true;
    }
}
