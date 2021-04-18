package org.cloudburstmc.server.enchantment.behavior.trident;

public class EnchantmentTridentImpaling extends EnchantmentTrident {

    @Override
    public int getMinEnchantAbility(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 15;
    }

}
