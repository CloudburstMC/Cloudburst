package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentProtectionProjectile extends EnchantmentProtection {

    public EnchantmentProtectionProjectile() {
        super(TYPE.PROJECTILE);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return 3 + (level - 1) * 6;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 15;
    }

    @Override
    public double getTypeModifier() {
        return 3;
    }

    @Override
    public float getProtectionFactor(EnchantmentInstance enchantment, EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();

        if (enchantment.getLevel() <= 0 || (cause != EntityDamageEvent.DamageCause.PROJECTILE)) {
            return 0;
        }

        return (float) (enchantment.getLevel() * getTypeModifier());
    }
}
