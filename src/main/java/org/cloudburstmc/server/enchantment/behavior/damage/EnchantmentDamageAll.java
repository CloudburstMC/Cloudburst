package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.entity.Entity;

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
        return enchantment.getLevel() * 1.25f; //https://minecraft.fandom.com/wiki/Sharpness#Usage
    }
}
