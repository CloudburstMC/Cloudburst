package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;

public final class EnchantmentProtectionFall extends EnchantmentProtection {

    public EnchantmentProtectionFall() {
        super(EnchantmentProtectionType.FALL);
    }

    @Override
    protected double getTypeModifier() {
        return 2;
    }

    @Override
    public float getProtectionFactor(Enchantment enchantment, EntityDamageEvent e) {
        DamageType damageType = e.getDamageType();

        if (enchantment.level() <= 0 || !damageType.is(DamageTypeTags.IS_FALL)) {
            return 0;
        }

        return (float) (enchantment.level() * getTypeModifier());
    }
}
