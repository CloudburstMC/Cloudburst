package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.CloudEffect;

import static org.cloudburstmc.server.item.ItemTypes.GLASS_BOTTLE;

public class FoodHoney extends Food {
    public FoodHoney(int restoreFood, float restoreSaturation) {
        this.setRestoreFood(restoreFood);
        this.setRestoreSaturation(restoreSaturation);
    }

    @Override
    protected boolean onEatenBy(CloudPlayer player) {
        super.onEatenBy(player);
        player.getInventory().addItem(ItemStack.get(GLASS_BOTTLE));
        player.removeEffect(CloudEffect.POISON);
        return true;
    }
}
