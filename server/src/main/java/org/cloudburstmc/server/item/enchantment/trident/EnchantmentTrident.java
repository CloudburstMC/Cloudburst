package org.cloudburstmc.server.item.enchantment.trident;

import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.item.enchantment.EnchantmentType;

public abstract class EnchantmentTrident extends Enchantment {
    protected EnchantmentTrident(int id, String name, int weight) {
        super(id, name, weight, EnchantmentType.TRIDENT);
    }

}
