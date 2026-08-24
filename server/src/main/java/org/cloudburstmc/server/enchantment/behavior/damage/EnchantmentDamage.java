package org.cloudburstmc.server.enchantment.behavior.damage;

import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

public abstract class EnchantmentDamage extends EnchantmentBehavior {

    @Override
    public boolean isMajor() {
        return true;
    }
}
