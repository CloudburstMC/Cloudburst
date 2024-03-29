package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;

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
        return item.getType() == ItemTypes.SHEARS || super.canEnchant(enchantment, item);
    }

}
