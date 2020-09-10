package org.cloudburstmc.server.enchantment;

import org.cloudburstmc.server.enchantment.behavior.EnchantmentBehavior;

public interface EnchantmentInstance {

    EnchantmentType getType();

    int getLevel();

    EnchantmentBehavior getBehavior();
}
