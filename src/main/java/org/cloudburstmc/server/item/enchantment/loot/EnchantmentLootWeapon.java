package org.cloudburstmc.server.item.enchantment.loot;

import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.item.enchantment.EnchantmentType;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentLootWeapon extends EnchantmentLoot {
    public EnchantmentLootWeapon() {
        super(Enchantment.ID_LOOTING, "lootBonus", 2, EnchantmentType.SWORD);
    }
}
