package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

public final class EnchantmentProtectionFire extends EnchantmentBehavior {

    @Override
    public float getDamageProtection(Enchantment enchantment, DamageSource source) {
        DamageType damageType = source.getDamageType();
        if (!damageType.is(DamageTypeTags.IS_FIRE) || damageType.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return 0;
        }

        return enchantment.level() * 2.0f;
    }

    @Override
    public float getBurningTimeReduction(Enchantment enchantment) {
        return enchantment.level() * 0.15f;
    }
}
