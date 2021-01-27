package org.cloudburstmc.api.enchantment;

import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;

public interface EnchantmentInstance {
    EnchantmentType getType();

    int getLevel();

    EnchantmentBehavior getBehavior();
}
