package org.cloudburstmc.server.item.enchantment.protection;

import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.enchantment.Enchantment;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentProtectionAll extends EnchantmentProtection {

    public EnchantmentProtectionAll() {
        super(Enchantment.ID_PROTECTION_ALL, "all", 10, TYPE.ALL);
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
    public float getProtectionFactor(EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();

        if (level <= 0 || cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.CUSTOM || cause == EntityDamageEvent.DamageCause.MAGIC) {
            return 0;
        }

        return (float) (getLevel() * getTypeModifier());
    }
}
