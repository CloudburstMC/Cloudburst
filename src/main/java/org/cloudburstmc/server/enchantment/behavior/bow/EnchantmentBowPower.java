package org.cloudburstmc.server.enchantment.behavior.bow;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentBowPower extends EnchantmentBow {

    @Override
    public int getMinEnchantAbility(int level) {
        return 1 + (level - 1) * 20;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 15;
    }
}
