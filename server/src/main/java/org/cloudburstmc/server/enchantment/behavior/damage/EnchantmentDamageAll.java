package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

public final class EnchantmentDamageAll extends EnchantmentBehavior {

    @Override
    public float modifyDamage(Enchantment enchantment, Entity target, float damage) {
        return damage + 1 + Math.max(enchantment.level() - 1, 0) * 0.5f;
    }
}
