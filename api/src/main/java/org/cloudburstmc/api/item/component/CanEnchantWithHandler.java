package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Tests whether an item accepts a specific enchantment.
 */
@FunctionalInterface
public interface CanEnchantWithHandler {

    /**
     * Returns whether the enchantment may be applied to the item.
     *
     * @param item        item being enchanted
     * @param enchantment enchantment type being applied
     * @return {@code true} if the item accepts the enchantment
     */
    boolean execute(ItemStack item, EnchantmentType enchantment);
}
