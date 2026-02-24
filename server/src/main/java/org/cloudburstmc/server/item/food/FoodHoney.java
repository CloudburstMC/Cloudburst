package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;

import static org.cloudburstmc.api.item.ItemTypes.GLASS_BOTTLE;

/**
 * Food behavior for honey bottles. On consumption, adds a glass bottle to the player's inventory
 * and removes the Poison effect.
 */
public class FoodHoney extends Food {
    public FoodHoney(int restoreFood, float restoreSaturation) {
        this.setRestoreFood(restoreFood);
        this.setRestoreSaturation(restoreSaturation);
    }

    @Override
    public boolean onEatenBy(Player player) {
        super.onEatenBy(player);
        ((CloudPlayerInventory) player.getInventory()).getContainer().addItem(ItemStack.from(GLASS_BOTTLE));
        player.removeEffect(EffectTypes.POISON);
        return true;
    }
}
