package org.cloudburstmc.server.enchantment.behavior;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentWaterWorker extends EnchantmentBehavior {

    @Override
    public int getMinEnchantAbility(int level) {
        return 1;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 40;
    }

}
