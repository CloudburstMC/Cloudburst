package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;

import static org.cloudburstmc.api.item.ItemTypes.BUCKET;

/**
 * Food behavior for milk buckets. On consumption, returns an empty bucket to the player's inventory
 * and removes all active effects.
 */
public class FoodMilk extends Food {
    @Override
    public boolean onEatenBy(Player player) {
        super.onEatenBy(player);
        ((CloudPlayerInventory) player.getInventory()).getContainer().addItem(ItemStack.from(BUCKET));
        player.removeAllEffects();
        return true;
    }
}
