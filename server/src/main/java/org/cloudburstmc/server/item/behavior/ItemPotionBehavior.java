package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.event.player.PlayerItemConsumeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.Potion;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class ItemPotionBehavior extends CloudItemBehavior {

    @Override
    public int getMaxStackSize(ItemStack item) {
        return 1;
    }

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        return true;
    }

    @Override
    public ItemStack onUse(ItemStack item, int ticksUsed, Player player) {
        PlayerItemConsumeEvent consumeEvent = new PlayerItemConsumeEvent(player, item);
        player.getServer().getEventManager().fire(consumeEvent);
        if (consumeEvent.isCancelled()) {
            return null;
        }
        Potion potion = item.getMetadata(Potion.class).setSplash(false);

        if (potion != null) {
            potion.applyPotion(player);
        }

        if (player.getGamemode() == GameMode.SURVIVAL) {
            player.getInventory().decrementHandCount();
            player.getInventory().addItem(CloudItemRegistry.get().getItem(ItemTypes.GLASS_BOTTLE));
        }

        return null;
    }
}