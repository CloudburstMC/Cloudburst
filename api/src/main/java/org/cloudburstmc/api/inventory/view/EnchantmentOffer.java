package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.enchantment.Enchantment;

import java.util.List;

/**
 * An option offered by an enchanting table.
 *
 * @param requiredLevel minimum player level required to select the option
 * @param enchantments  enchantments applied by the option, with the displayed clue first
 */
public record EnchantmentOffer(int requiredLevel, List<Enchantment> enchantments) {

    public EnchantmentOffer {
        enchantments = List.copyOf(enchantments);
        if (requiredLevel <= 0 || enchantments.isEmpty()) {
            throw new IllegalArgumentException("An offer requires a positive level and at least one enchantment");
        }
    }
}
