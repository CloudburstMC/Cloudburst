package org.cloudburstmc.server.enchantment.behavior.trident;

public class EnchantmentTridentLoyalty extends EnchantmentTrident {

    @Override
    public int getMinEnchantAbility(int level) {
        return level * 10;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 15;
    }

}
