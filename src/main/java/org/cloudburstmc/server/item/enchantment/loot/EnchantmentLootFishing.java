package org.cloudburstmc.server.item.enchantment.loot;

import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.item.enchantment.EnchantmentType;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantmentLootFishing extends EnchantmentLoot {
    public EnchantmentLootFishing() {
        super(Enchantment.ID_FORTUNE_FISHING, "lootBonusFishing", 2, EnchantmentType.FISHING_ROD);
    }
}
