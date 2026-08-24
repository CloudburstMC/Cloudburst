package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;

public final class EnchantmentDamageAll extends EnchantmentDamage {

    @Override
    public float getDamageBonus(Enchantment enchantment, Entity entity) {
        return enchantment.level() * 1.25f;
    }
}
