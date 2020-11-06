package org.cloudburstmc.server.enchantment.behavior;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentWaterBreath extends EnchantmentBehavior {

    @Override
    public int getMinEnchantAbility(int level) {
        return 10 + (level - 1) * 20;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 50;
    }

}
