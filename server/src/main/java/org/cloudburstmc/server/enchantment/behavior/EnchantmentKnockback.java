package org.cloudburstmc.server.enchantment.behavior;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;

public final class EnchantmentKnockback extends EnchantmentBehavior {

    @Override
    public float modifyKnockback(Enchantment enchantment, Entity target, float knockback) {
        return knockback + enchantment.level();
    }
}
