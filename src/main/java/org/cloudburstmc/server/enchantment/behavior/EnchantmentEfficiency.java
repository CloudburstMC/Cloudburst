package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentEfficiency extends EnchantmentBehavior {

    @Override
    public int getMinEnchantAbility(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 50;
    }

    @Override
    public boolean canEnchant(EnchantmentInstance enchantment, ItemStack item) {
        return item.getBehavior().isShears() || super.canEnchant(enchantment, item);
    }

}
