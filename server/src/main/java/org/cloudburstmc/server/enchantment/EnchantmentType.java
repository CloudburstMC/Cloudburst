package org.cloudburstmc.server.enchantment;

import org.cloudburstmc.api.util.Identifier;

public interface EnchantmentType {

    Identifier getType();

    int getMaxLevel();

    int getWeight();

    EnchantmentTarget getTarget();
}
