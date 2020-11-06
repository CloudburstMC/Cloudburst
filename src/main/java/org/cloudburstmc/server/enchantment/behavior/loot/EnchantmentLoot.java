package org.cloudburstmc.server.enchantment.behavior.loot;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentTypes;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentLoot extends EnchantmentBehavior {

    @Override
    public int getMinEnchantAbility(int level) {
        return 15 + (level - 1) * 9;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 50;
    }

    @Override
    public boolean isCompatibleWith(EnchantmentInstance first, EnchantmentInstance enchantment) {
        return super.isCompatibleWith(first, enchantment) && enchantment.getType() != EnchantmentTypes.SILK_TOUCH;
    }
}
