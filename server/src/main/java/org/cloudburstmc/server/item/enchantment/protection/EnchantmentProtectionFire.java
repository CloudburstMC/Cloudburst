package org.cloudburstmc.server.item.enchantment.protection;

import org.cloudburstmc.server.event.entity.EntityDamageEvent;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentProtectionFire extends EnchantmentProtection {

    public EnchantmentProtectionFire() {
        super(ID_PROTECTION_FIRE, "fire", 5, TYPE.FIRE);
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
    public float getProtectionFactor(EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();

        if (level <= 0 || (cause != EntityDamageEvent.DamageCause.LAVA && cause != EntityDamageEvent.DamageCause.FIRE && cause != EntityDamageEvent.DamageCause.FIRE_TICK)) {
            return 0;
        }

        return (float) (getLevel() * getTypeModifier());
    }
}
