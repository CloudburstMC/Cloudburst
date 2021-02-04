package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;

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
