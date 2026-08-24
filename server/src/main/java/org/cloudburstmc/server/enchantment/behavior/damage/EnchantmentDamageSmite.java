package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Smiteable;

public final class EnchantmentDamageSmite extends EnchantmentDamage {

    @Override
    public float getDamageBonus(Enchantment enchantment, Entity entity) {
        if (entity instanceof Smiteable) {
            return enchantment.level() * 2.5f;
        }

        return 0;
    }
}
