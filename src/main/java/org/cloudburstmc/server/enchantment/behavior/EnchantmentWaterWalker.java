package org.cloudburstmc.server.enchantment.behavior;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentWaterWalker extends EnchantmentBehavior {

    @Override
    public int getMinEnchantAbility(int level) {
        return level * 10;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 15;
    }

}
