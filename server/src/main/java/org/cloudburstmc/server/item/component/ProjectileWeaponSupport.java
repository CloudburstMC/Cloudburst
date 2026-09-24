package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProjectileWeaponSupport {

    public static final int OFFHAND = -1;
    public static final int NO_AMMO = -2;

    public static int findArrowSlot(CloudPlayer player) {
        if (player.getOffhand().getOffhandItem().getType() == ItemTypes.ARROW) {
            return OFFHAND;
        }

        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getItem(slot).getType() == ItemTypes.ARROW) {
                return slot;
            }
        }

        return NO_AMMO;
    }

    public static void consumeArrow(CloudPlayer player, int slot) {
        if (slot == OFFHAND) {
            ItemStack arrow = player.getOffhand().getOffhandItem();
            player.getOffhand().setOffhandItem(arrow.decreaseCount());
        } else {
            ItemStack arrow = player.getInventory().getItem(slot);
            player.getInventory().setItem(slot, arrow.decreaseCount());
        }
    }

    public static int enchantmentLevel(ItemStack item, EnchantmentType type) {
        Map<EnchantmentType, Enchantment> enchantments = item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of());
        Enchantment enchantment = enchantments.get(type);
        return enchantment == null ? 0 : enchantment.level();
    }
}
