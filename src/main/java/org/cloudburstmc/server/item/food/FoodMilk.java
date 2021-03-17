package org.cloudburstmc.server.item.food;

import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.server.item.ItemTypes.BUCKET;

/**
 * Created by Snake1999 on 2016/1/21.
 * Package cn.nukkit.item.food in project nukkit.
 */
public class FoodMilk extends Food {
    @Override
    protected boolean onEatenBy(CloudPlayer player) {
        super.onEatenBy(player);
        player.getInventory().addItem(CloudItemRegistry.get().getItem(BUCKET));
        player.removeAllEffects();
        return true;
    }
}
