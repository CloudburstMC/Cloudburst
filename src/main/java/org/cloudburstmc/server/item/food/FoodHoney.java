package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.item.ItemTypes.GLASS_BOTTLE;

public class FoodHoney extends Food {
    public FoodHoney(int restoreFood, float restoreSaturation) {
        this.setRestoreFood(restoreFood);
        this.setRestoreSaturation(restoreSaturation);
    }

    @Override
    public boolean onEatenBy(Player player) {
        super.onEatenBy(player);
        player.getInventory().addItem(CloudItemRegistry.get().getItem(GLASS_BOTTLE));
        player.removeEffect(EffectTypes.POISON);
        return true;
    }
}
