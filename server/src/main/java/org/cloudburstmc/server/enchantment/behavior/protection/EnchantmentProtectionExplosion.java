package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

public final class EnchantmentProtectionExplosion extends EnchantmentBehavior {

    @Override
    public float getDamageProtection(Enchantment enchantment, EntityDamageEvent event) {
        DamageType damageType = event.getDamageType();
        if (!damageType.is(DamageTypeTags.IS_EXPLOSION) || damageType.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return 0;
        }

        return enchantment.level() * 2;
    }
}
