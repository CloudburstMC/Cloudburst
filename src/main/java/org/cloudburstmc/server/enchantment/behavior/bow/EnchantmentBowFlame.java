package org.cloudburstmc.server.enchantment.behavior.bow;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentBowFlame extends EnchantmentBow {

    @Override
    public int getMinEnchantAbility(int level) {
        return 20;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return 50;
    }
}
