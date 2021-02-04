package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentProtectionExplosion extends EnchantmentProtection {

    public EnchantmentProtectionExplosion() {
        super(TYPE.EXPLOSION);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return 5 + (level - 1) * 8;
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
        EntityDamageEvent.DamageCause cause = e.getCause();

        if (enchantment.getLevel() <= 0 || (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            return 0;
        }

        return (float) (enchantment.getLevel() * getTypeModifier());
    }
}
