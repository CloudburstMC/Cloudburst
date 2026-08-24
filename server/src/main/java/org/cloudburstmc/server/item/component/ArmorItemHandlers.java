package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

@UtilityClass
public class ArmorItemHandlers {

    public static UseHandler equip(EquipmentSlot slot, Sound equipSound) {
        if (!slot.isArmor()) {
            throw new IllegalArgumentException("slot must be an armor slot");
        }

        return equip(slot.getArmorSlot(), equipSound);
    }

    public static int armorSlot(ItemStack item) {
        if (item.isEmpty()) {
            return -1;
        }

        EquipmentSlot slot = CloudItemRegistry.get().requireComponent(item.getType(), ItemComponents.GET_EQUIPMENT_SLOT).execute(item);
        return slot == null || !slot.isArmor() ? -1 : slot.getArmorSlot();
    }

    private static UseHandler equip(int armorSlot, Sound equipSound) {
        return (itemStack, entity) -> {
            if (!(entity instanceof CloudPlayer player)) {
                return itemStack;
            }

            ArmorView armor = player.getArmor();
            ItemStack equipped = armor.getItem(armorSlot);
            if (itemStack.isSimilarMetadata(equipped)) {
                return itemStack;
            }

            ItemStack itemToEquip = itemStack.withCount(1);
            armor.setItem(armorSlot, itemToEquip);
            player.getItemStackNetManager().recordServerAuthoritativeArmorUse(armorSlot);
            player.getLevel().addSound(player.getPosition(), equipSound);

            if (itemStack.getCount() == 1) {
                return equipped;
            }

            if (!equipped.isEmpty()) {
                ItemStack[] leftovers = player.getContainer().addItem(equipped);
                for (ItemStack leftover : leftovers) {
                    player.dropItem(leftover);
                }
            }

            return itemStack.decreaseCount();
        };
    }
}
