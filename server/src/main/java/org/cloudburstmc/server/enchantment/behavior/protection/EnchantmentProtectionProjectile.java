package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;

public final class EnchantmentProtectionProjectile extends EnchantmentProtection {

    public EnchantmentProtectionProjectile() {
        super(EnchantmentProtectionType.PROJECTILE);
    }

    @Override
    protected double getTypeModifier() {
        return 3;
    }

    @Override
    public float getProtectionFactor(Enchantment enchantment, EntityDamageEvent e) {
        DamageType damageType = e.getDamageType();

        if (enchantment.level() <= 0 || !damageType.is(DamageTypeTags.IS_PROJECTILE)) {
            return 0;
        }

        return (float) (enchantment.level() * getTypeModifier());
    }
}
