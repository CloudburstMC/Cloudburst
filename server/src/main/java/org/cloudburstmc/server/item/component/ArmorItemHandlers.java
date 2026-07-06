package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Map;

@UtilityClass
public class ArmorItemHandlers {

    private static final int HELMET_SLOT = 0;
    private static final int CHESTPLATE_SLOT = 1;
    private static final int LEGGINGS_SLOT = 2;
    private static final int BOOTS_SLOT = 3;

    private static final Map<ItemType, ArmorProperties> ARMOR_PROPERTIES = Map.ofEntries(
            armor(ItemTypes.CHAINMAIL_HELMET, HELMET_SLOT, Sound.ARMOR_EQUIP_CHAIN),
            armor(ItemTypes.CHAINMAIL_CHESTPLATE, CHESTPLATE_SLOT, Sound.ARMOR_EQUIP_CHAIN),
            armor(ItemTypes.CHAINMAIL_LEGGINGS, LEGGINGS_SLOT, Sound.ARMOR_EQUIP_CHAIN),
            armor(ItemTypes.CHAINMAIL_BOOTS, BOOTS_SLOT, Sound.ARMOR_EQUIP_CHAIN),
            armor(ItemTypes.COPPER_HELMET, HELMET_SLOT, Sound.ARMOR_EQUIP_COPPER),
            armor(ItemTypes.COPPER_CHESTPLATE, CHESTPLATE_SLOT, Sound.ARMOR_EQUIP_COPPER),
            armor(ItemTypes.COPPER_LEGGINGS, LEGGINGS_SLOT, Sound.ARMOR_EQUIP_COPPER),
            armor(ItemTypes.COPPER_BOOTS, BOOTS_SLOT, Sound.ARMOR_EQUIP_COPPER),
            armor(ItemTypes.DIAMOND_HELMET, HELMET_SLOT, Sound.ARMOR_EQUIP_DIAMOND),
            armor(ItemTypes.DIAMOND_CHESTPLATE, CHESTPLATE_SLOT, Sound.ARMOR_EQUIP_DIAMOND),
            armor(ItemTypes.DIAMOND_LEGGINGS, LEGGINGS_SLOT, Sound.ARMOR_EQUIP_DIAMOND),
            armor(ItemTypes.DIAMOND_BOOTS, BOOTS_SLOT, Sound.ARMOR_EQUIP_DIAMOND),
            armor(ItemTypes.GOLDEN_HELMET, HELMET_SLOT, Sound.ARMOR_EQUIP_GOLD),
            armor(ItemTypes.GOLDEN_CHESTPLATE, CHESTPLATE_SLOT, Sound.ARMOR_EQUIP_GOLD),
            armor(ItemTypes.GOLDEN_LEGGINGS, LEGGINGS_SLOT, Sound.ARMOR_EQUIP_GOLD),
            armor(ItemTypes.GOLDEN_BOOTS, BOOTS_SLOT, Sound.ARMOR_EQUIP_GOLD),
            armor(ItemTypes.IRON_HELMET, HELMET_SLOT, Sound.ARMOR_EQUIP_IRON),
            armor(ItemTypes.IRON_CHESTPLATE, CHESTPLATE_SLOT, Sound.ARMOR_EQUIP_IRON),
            armor(ItemTypes.IRON_LEGGINGS, LEGGINGS_SLOT, Sound.ARMOR_EQUIP_IRON),
            armor(ItemTypes.IRON_BOOTS, BOOTS_SLOT, Sound.ARMOR_EQUIP_IRON),
            armor(ItemTypes.LEATHER_HELMET, HELMET_SLOT, Sound.ARMOR_EQUIP_LEATHER),
            armor(ItemTypes.LEATHER_CHESTPLATE, CHESTPLATE_SLOT, Sound.ARMOR_EQUIP_LEATHER),
            armor(ItemTypes.LEATHER_LEGGINGS, LEGGINGS_SLOT, Sound.ARMOR_EQUIP_LEATHER),
            armor(ItemTypes.LEATHER_BOOTS, BOOTS_SLOT, Sound.ARMOR_EQUIP_LEATHER),
            armor(ItemTypes.NETHERITE_HELMET, HELMET_SLOT, Sound.ARMOR_EQUIP_NETHERITE),
            armor(ItemTypes.NETHERITE_CHESTPLATE, CHESTPLATE_SLOT, Sound.ARMOR_EQUIP_NETHERITE),
            armor(ItemTypes.NETHERITE_LEGGINGS, LEGGINGS_SLOT, Sound.ARMOR_EQUIP_NETHERITE),
            armor(ItemTypes.NETHERITE_BOOTS, BOOTS_SLOT, Sound.ARMOR_EQUIP_NETHERITE),
            armor(ItemTypes.ELYTRA, CHESTPLATE_SLOT, Sound.ARMOR_EQUIP_ELYTRA),
            armor(ItemTypes.TURTLE_HELMET, HELMET_SLOT, Sound.ARMOR_EQUIP_GENERIC)
    );

    public static UseHandler helmet() {
        return equip(HELMET_SLOT);
    }

    public static UseHandler chestplate() {
        return equip(CHESTPLATE_SLOT);
    }

    public static UseHandler leggings() {
        return equip(LEGGINGS_SLOT);
    }

    public static UseHandler boots() {
        return equip(BOOTS_SLOT);
    }

    public static int armorSlot(ItemStack item) {
        if (item.isEmpty()) {
            return -1;
        }

        ArmorProperties properties = ARMOR_PROPERTIES.get(item.getType());
        return properties == null ? -1 : properties.slot();
    }

    private static UseHandler equip(int armorSlot) {
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
            player.getLevel().addSound(player.getPosition(), equipSound(itemStack.getType()));

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

    private static Sound equipSound(ItemType type) {
        ArmorProperties properties = ARMOR_PROPERTIES.get(type);
        return properties == null ? Sound.ARMOR_EQUIP_GENERIC : properties.equipSound();
    }

    private static Map.Entry<ItemType, ArmorProperties> armor(ItemType type, int slot, Sound equipSound) {
        return Map.entry(type, new ArmorProperties(slot, equipSound));
    }

    private record ArmorProperties(int slot, Sound equipSound) {
    }
}
