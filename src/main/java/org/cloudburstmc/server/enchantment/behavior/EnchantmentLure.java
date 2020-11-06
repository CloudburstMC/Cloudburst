package org.cloudburstmc.server.enchantment.behavior;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentLure extends EnchantmentBehavior {

    @Override
    public int getMinEnchantAbility(int level) {
        return 15 + (level - 1) * 9;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 50;
    }

}
