package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;

import static org.cloudburstmc.api.item.ItemTypes.BOWL;

/**
 * Food behavior for bowl-based foods such as mushroom stew. On consumption, returns an empty bowl
 * to the player's inventory.
 */
public class FoodInBowl extends Food {

    public FoodInBowl(int restoreFood, float restoreSaturation) {
        this.setRestoreFood(restoreFood);
        this.setRestoreSaturation(restoreSaturation);
    }

    @Override
    public boolean onEatenBy(Player player) {
        super.onEatenBy(player);
        ((CloudPlayerInventory) player.getInventory()).getContainer().addItem(ItemStack.from(BOWL));
        return true;
    }

}
