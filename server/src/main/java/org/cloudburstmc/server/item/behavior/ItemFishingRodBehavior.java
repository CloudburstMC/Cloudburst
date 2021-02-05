package org.cloudburstmc.server.item.behavior;

import lombok.val;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.item.data.Damageable;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * Created by Snake1999 on 2016/1/14.
 * Package cn.nukkit.item in project nukkit.
 */
public class ItemFishingRodBehavior extends ItemToolBehavior {

    public ItemFishingRodBehavior() {
        super(null, null);
    }

    @Override
    public int getEnchantAbility(ItemStack item) {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack item) {
        return 1;
    }

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, CloudPlayer player) {
        return true;
    }

    @Override
    public ItemStack onUse(ItemStack item, int ticksUsed, CloudPlayer player) {
        if (player.fishing != null) {
            player.stopFishing(true);
        } else {
            player.startFishing(item);
            val damage = item.getMetadata(Damageable.class);
            if (damage != null) {
                return item.withData(damage.damage());
            }
        }

        return null;
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_FISHING_ROD;
    }
}
