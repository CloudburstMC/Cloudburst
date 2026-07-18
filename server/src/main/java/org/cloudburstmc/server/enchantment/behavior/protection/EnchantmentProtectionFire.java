package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentProtectionFire extends EnchantmentProtection {

    public EnchantmentProtectionFire() {
        super(TYPE.FIRE);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return 10 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 12;
    }

    @Override
    public double getTypeModifier() {
        return 2;
    }

    @Override
    public float getProtectionFactor(EnchantmentInstance enchantment, EntityDamageEvent e) {
        DamageType damageType = e.getDamageType();

        if (enchantment.getLevel() <= 0 || !damageType.is(DamageTypeTags.IS_FIRE)) {
            return 0;
        }

        return (float) (enchantment.getLevel() * getTypeModifier());
    }
}
