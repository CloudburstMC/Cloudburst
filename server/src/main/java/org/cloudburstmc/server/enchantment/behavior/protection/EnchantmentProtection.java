package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

public abstract class EnchantmentProtection extends EnchantmentBehavior {

    protected final EnchantmentProtectionType protectionType;

    protected EnchantmentProtection(EnchantmentProtectionType type) {
        this.protectionType = type;
    }

    protected abstract double getTypeModifier();

    @Override
    public boolean isMajor() {
        return true;
    }

    public abstract float getProtectionFactor(Enchantment enchantment, EntityDamageEvent e);
}
