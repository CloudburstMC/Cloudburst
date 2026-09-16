package org.cloudburstmc.server.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.item.ItemTags;
import org.cloudburstmc.server.level.Sound;

/**
 * Vanilla armor material definitions.
 */
@UtilityClass
public class VanillaArmorMaterials {
    public static final ArmorMaterial LEATHER = new ArmorMaterial(5, 1, 2, 3, 1, 0, 0, ItemTags.REPAIRS_LEATHER_ARMOR, Sound.ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial COPPER = new ArmorMaterial(11, 1, 3, 4, 2, 0, 0, ItemTags.REPAIRS_COPPER_ARMOR, Sound.ARMOR_EQUIP_COPPER);
    public static final ArmorMaterial CHAINMAIL = new ArmorMaterial(15, 1, 4, 5, 2, 0, 0, ItemTags.REPAIRS_CHAIN_ARMOR, Sound.ARMOR_EQUIP_CHAIN);
    public static final ArmorMaterial IRON = new ArmorMaterial(15, 2, 5, 6, 2, 0, 0, ItemTags.REPAIRS_IRON_ARMOR, Sound.ARMOR_EQUIP_IRON);
    public static final ArmorMaterial GOLD = new ArmorMaterial(7, 1, 3, 5, 2, 0, 0, ItemTags.REPAIRS_GOLD_ARMOR, Sound.ARMOR_EQUIP_GOLD);
    public static final ArmorMaterial DIAMOND = new ArmorMaterial(33, 3, 6, 8, 3, 2, 0, ItemTags.REPAIRS_DIAMOND_ARMOR, Sound.ARMOR_EQUIP_DIAMOND);
    public static final ArmorMaterial TURTLE = new ArmorMaterial(25, 2, 5, 6, 2, 0, 0, ItemTags.REPAIRS_TURTLE_HELMET, Sound.ARMOR_EQUIP_GENERIC);
    public static final ArmorMaterial NETHERITE = new ArmorMaterial(37, 3, 6, 8, 3, 3, 0.1f, ItemTags.REPAIRS_NETHERITE_ARMOR, Sound.ARMOR_EQUIP_NETHERITE);
}
