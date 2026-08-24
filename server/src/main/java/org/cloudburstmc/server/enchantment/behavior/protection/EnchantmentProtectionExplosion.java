package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;

public final class EnchantmentProtectionExplosion extends EnchantmentProtection {

    public EnchantmentProtectionExplosion() {
        super(EnchantmentProtectionType.EXPLOSION);
    }

    @Override
    protected double getTypeModifier() {
        return 2;
    }

    @Override
    public float getProtectionFactor(Enchantment enchantment, EntityDamageEvent e) {
        DamageType damageType = e.getDamageType();

        if (enchantment.level() <= 0 || !damageType.is(DamageTypeTags.IS_EXPLOSION)) {
            return 0;
        }

        return (float) (enchantment.level() * getTypeModifier());
    }
}
