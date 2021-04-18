package org.cloudburstmc.server.enchantment.behavior.protection;

import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EnchantmentProtection extends EnchantmentBehavior {

    public enum TYPE {
        ALL,
        FIRE,
        FALL,
        EXPLOSION,
        PROJECTILE
    }

    protected final TYPE protectionType;

    protected EnchantmentProtection(EnchantmentProtection.TYPE type) {
        this.protectionType = type;
    }

    @Override
    public boolean isCompatibleWith(EnchantmentInstance first, EnchantmentInstance enchantment) {
        var behavior = enchantment.getBehavior();
        if (behavior instanceof EnchantmentProtection) {
            if (((EnchantmentProtection) behavior).protectionType == this.protectionType) {
                return false;
            }
            return ((EnchantmentProtection) behavior).protectionType == TYPE.FALL || this.protectionType == TYPE.FALL;
        }
        return super.isCompatibleWith(first, enchantment);
    }

    public double getTypeModifier() {
        return 0;
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    public abstract float getProtectionFactor(EnchantmentInstance enchantment, EntityDamageEvent e);
}
