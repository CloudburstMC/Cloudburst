package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;

/**
 * Built-in smithing upgrade pairings for all standard upgradeable items.
 * Each constant maps a base item to its upgraded equivalent.
 */
@UtilityClass
public final class SmithingUpgrades {
    public static final SmithingUpgrade AXE = new SmithingUpgrade(Identifier.parse("smithing_netherite_axe"), ItemIds.DIAMOND_AXE, ItemIds.NETHERITE_AXE);
    public static final SmithingUpgrade BOOTS = new SmithingUpgrade(Identifier.parse("smithing_netherite_boots"), ItemIds.DIAMOND_BOOTS, ItemIds.NETHERITE_BOOTS);
    public static final SmithingUpgrade CHESTPLATE = new SmithingUpgrade(Identifier.parse("smithing_netherite_chestplate"), ItemIds.DIAMOND_CHESTPLATE, ItemIds.NETHERITE_CHESTPLATE);
    public static final SmithingUpgrade HELMET = new SmithingUpgrade(Identifier.parse("smithing_netherite_helmet"), ItemIds.DIAMOND_HELMET, ItemIds.NETHERITE_HELMET);
    public static final SmithingUpgrade HOE = new SmithingUpgrade(Identifier.parse("smithing_netherite_hoe"), ItemIds.DIAMOND_HOE, ItemIds.NETHERITE_HOE);
    public static final SmithingUpgrade HORSE_ARMOR = new SmithingUpgrade(Identifier.parse("smithing_netherite_horse_armor"), ItemIds.DIAMOND_HORSE_ARMOR, ItemIds.NETHERITE_HORSE_ARMOR);
    public static final SmithingUpgrade LEGGINGS = new SmithingUpgrade(Identifier.parse("smithing_netherite_leggings"), ItemIds.DIAMOND_LEGGINGS, ItemIds.NETHERITE_LEGGINGS);
    public static final SmithingUpgrade NAUTILUS_ARMOR = new SmithingUpgrade(Identifier.parse("smithing_netherite_nautilus_armor"), ItemIds.DIAMOND_NAUTILUS_ARMOR, ItemIds.NETHERITE_NAUTILUS_ARMOR);
    public static final SmithingUpgrade PICKAXE = new SmithingUpgrade(Identifier.parse("smithing_netherite_pickaxe"), ItemIds.DIAMOND_PICKAXE, ItemIds.NETHERITE_PICKAXE);
    public static final SmithingUpgrade SHOVEL = new SmithingUpgrade(Identifier.parse("smithing_netherite_shovel"), ItemIds.DIAMOND_SHOVEL, ItemIds.NETHERITE_SHOVEL);
    public static final SmithingUpgrade SPEAR = new SmithingUpgrade(Identifier.parse("smithing_netherite_spear"), ItemIds.DIAMOND_SPEAR, ItemIds.NETHERITE_SPEAR);
    public static final SmithingUpgrade SWORD = new SmithingUpgrade(Identifier.parse("smithing_netherite_sword"), ItemIds.DIAMOND_SWORD, ItemIds.NETHERITE_SWORD);
}
