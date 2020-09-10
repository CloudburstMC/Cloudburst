package org.cloudburstmc.server.enchantment.behavior.protection;

import lombok.val;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

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
        val behavior = enchantment.getBehavior();
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
}
