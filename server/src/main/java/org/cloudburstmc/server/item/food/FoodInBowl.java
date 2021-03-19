package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.registry.CloudItemRegistry;

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
    public boolean onEatenBy(Player player) {
        super.onEatenBy(player);
        player.getInventory().addItem(CloudItemRegistry.get().getItem(BOWL));
        return true;
    }

}
