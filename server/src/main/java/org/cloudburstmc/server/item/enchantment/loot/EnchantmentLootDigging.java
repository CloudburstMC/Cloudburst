package org.cloudburstmc.server.item.enchantment.loot;

import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.item.enchantment.EnchantmentType;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentLootDigging extends EnchantmentLoot {
    public EnchantmentLootDigging() {
        super(Enchantment.ID_FORTUNE_DIGGING, "lootBonusDigger", 2, EnchantmentType.DIGGER);
    }
}
