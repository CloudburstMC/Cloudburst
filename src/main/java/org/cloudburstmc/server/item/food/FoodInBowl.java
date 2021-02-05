package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.server.item.ItemTypes.BOWL;

/**
 * Created by Snake1999 on 2016/1/14.
 * Package cn.nukkit.item.food in project nukkit.
 */
public class FoodInBowl extends Food {

    public FoodInBowl(int restoreFood, float restoreSaturation) {
        this.setRestoreFood(restoreFood);
        this.setRestoreSaturation(restoreSaturation);
    }

    @Override
    protected boolean onEatenBy(CloudPlayer player) {
        super.onEatenBy(player);
        player.getInventory().addItem(ItemStack.get(BOWL));
        return true;
    }

}
