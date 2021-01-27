package org.cloudburstmc.api.enchantment;

import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.api.item.ItemStack;

public interface EnchantmentInstance {
    EnchantmentType getType();

    int getLevel();

    EnchantmentBehavior getBehavior();

    boolean canEnchantItem(ItemStack item);
}
