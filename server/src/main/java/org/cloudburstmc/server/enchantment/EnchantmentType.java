package org.cloudburstmc.server.enchantment;

import org.cloudburstmc.server.utils.Identifier;

public interface EnchantmentType {

    Identifier getType();

    int getMaxLevel();

    int getWeight();

    EnchantmentTarget getTarget();
}
