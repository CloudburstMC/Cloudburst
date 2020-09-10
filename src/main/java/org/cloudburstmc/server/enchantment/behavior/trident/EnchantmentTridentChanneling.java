package org.cloudburstmc.server.enchantment.behavior.trident;

public class EnchantmentTridentChanneling extends EnchantmentTrident {

    @Override
    public int getMinEnchantAbility(int level) {
        return 20;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 50;
    }
}
