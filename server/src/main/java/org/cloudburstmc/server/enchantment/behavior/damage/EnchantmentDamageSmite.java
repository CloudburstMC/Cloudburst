package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Smiteable;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

public final class EnchantmentDamageSmite extends EnchantmentBehavior {

    @Override
    public float modifyDamage(Enchantment enchantment, Entity target, float damage) {
        if (target instanceof Smiteable) {
            return damage + enchantment.level() * 2.5f;
        }

        return damage;
    }
}
