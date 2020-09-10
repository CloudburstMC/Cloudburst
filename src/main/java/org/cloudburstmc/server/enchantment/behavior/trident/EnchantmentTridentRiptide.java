package org.cloudburstmc.server.enchantment.behavior.trident;

public class EnchantmentTridentRiptide extends EnchantmentTrident {

    @Override
    public int getMinEnchantAbility(int level) {
        return level * 10;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 15;
    }

}
