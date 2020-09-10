package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.entity.Entity;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentDamageAll extends EnchantmentDamage {

    public EnchantmentDamageAll() {
        super(TYPE.ALL);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return 1 + (level - 1) * 11;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 20;
    }

    @Override
    public int getMaxEnchantableLevel(EnchantmentInstance enchantment) {
        return 4;
    }

    @Override
    public float getDamageBonus(EnchantmentInstance enchantment, Entity entity) {
        if (enchantment.getLevel() <= 0) {
            return 0;
        }

        return 0.5f + enchantment.getLevel() * 0.5f;
    }
}
