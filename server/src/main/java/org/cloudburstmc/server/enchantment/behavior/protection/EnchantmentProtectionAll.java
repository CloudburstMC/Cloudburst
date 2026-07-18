package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;

public class EnchantmentProtectionAll extends EnchantmentProtection {

    public EnchantmentProtectionAll() {
        super(TYPE.ALL);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return 1 + (level - 1) * 11;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 20;
    }

    @Override
    public double getTypeModifier() {
        return 1;
    }

    @Override
    public float getProtectionFactor(EnchantmentInstance enchantment, EntityDamageEvent e) {
        DamageType damageType = e.getDamageType();
        if (enchantment.getLevel() <= 0 || damageType.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return 0;
        }

        return (float) (enchantment.getLevel() * getTypeModifier());
    }
}
