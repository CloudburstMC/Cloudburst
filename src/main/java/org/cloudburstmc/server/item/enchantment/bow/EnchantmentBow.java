package org.cloudburstmc.server.item.enchantment.bow;

import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.item.enchantment.EnchantmentType;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EnchantmentBow extends Enchantment {
    protected EnchantmentBow(int id, String name, int weight) {
        super(id, name, weight, EnchantmentType.BOW);
    }

}
