package org.cloudburstmc.server.item.enchantment.protection;

import org.cloudburstmc.server.event.entity.EntityDamageEvent;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentProtectionFall extends EnchantmentProtection {

    public EnchantmentProtectionFall() {
        super(ID_PROTECTION_FALL, "fall", 5, TYPE.FALL);
    }

    @Override
    public int getMinEnchantAbility(int level) {
        return 5 + (level - 1) * 6;
    }

    @Override
    public int getMaxEnchantAbility(int level) {
        return this.getMinEnchantAbility(level) + 10;
    }

    @Override
    public double getTypeModifier() {
        return 2;
    }

    @Override
    public float getProtectionFactor(EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();

        if (level <= 0 || (cause != EntityDamageEvent.DamageCause.FALL)) {
            return 0;
        }

        return (float) (getLevel() * getTypeModifier());
    }
}
