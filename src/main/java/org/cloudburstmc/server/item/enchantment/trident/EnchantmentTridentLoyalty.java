package org.cloudburstmc.server.item.enchantment.trident;

import org.cloudburstmc.server.item.enchantment.Enchantment;

public class EnchantmentTridentLoyalty extends EnchantmentTrident {
    public EnchantmentTridentLoyalty() {
        super(Enchantment.ID_TRIDENT_LOYALTY, "loyalty", 5);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return level * 10;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 15;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
