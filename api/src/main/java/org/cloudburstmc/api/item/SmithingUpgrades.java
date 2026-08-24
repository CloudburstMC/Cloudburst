package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;

/**
 * Built-in smithing upgrade pairings for all standard upgradeable items.
 * Each constant maps a base item to its upgraded equivalent.
 */
@UtilityClass
public final class SmithingUpgrades {
    public static final SmithingUpgrade AXE = new SmithingUpgrade(Identifier.parse("smithing_netherite_axe"), ItemTypes.DIAMOND_AXE.getId(), ItemTypes.NETHERITE_AXE.getId());
    public static final SmithingUpgrade BOOTS = new SmithingUpgrade(Identifier.parse("smithing_netherite_boots"), ItemTypes.DIAMOND_BOOTS.getId(), ItemTypes.NETHERITE_BOOTS.getId());
    public static final SmithingUpgrade CHESTPLATE = new SmithingUpgrade(Identifier.parse("smithing_netherite_chestplate"), ItemTypes.DIAMOND_CHESTPLATE.getId(), ItemTypes.NETHERITE_CHESTPLATE.getId());
    public static final SmithingUpgrade HELMET = new SmithingUpgrade(Identifier.parse("smithing_netherite_helmet"), ItemTypes.DIAMOND_HELMET.getId(), ItemTypes.NETHERITE_HELMET.getId());
    public static final SmithingUpgrade HOE = new SmithingUpgrade(Identifier.parse("smithing_netherite_hoe"), ItemTypes.DIAMOND_HOE.getId(), ItemTypes.NETHERITE_HOE.getId());
    public static final SmithingUpgrade HORSE_ARMOR = new SmithingUpgrade(Identifier.parse("smithing_netherite_horse_armor"), ItemTypes.DIAMOND_HORSE_ARMOR.getId(), ItemTypes.NETHERITE_HORSE_ARMOR.getId());
    public static final SmithingUpgrade LEGGINGS = new SmithingUpgrade(Identifier.parse("smithing_netherite_leggings"), ItemTypes.DIAMOND_LEGGINGS.getId(), ItemTypes.NETHERITE_LEGGINGS.getId());
    public static final SmithingUpgrade NAUTILUS_ARMOR = new SmithingUpgrade(Identifier.parse("smithing_netherite_nautilus_armor"), ItemTypes.DIAMOND_NAUTILUS_ARMOR.getId(), ItemTypes.NETHERITE_NAUTILUS_ARMOR.getId());
    public static final SmithingUpgrade PICKAXE = new SmithingUpgrade(Identifier.parse("smithing_netherite_pickaxe"), ItemTypes.DIAMOND_PICKAXE.getId(), ItemTypes.NETHERITE_PICKAXE.getId());
    public static final SmithingUpgrade SHOVEL = new SmithingUpgrade(Identifier.parse("smithing_netherite_shovel"), ItemTypes.DIAMOND_SHOVEL.getId(), ItemTypes.NETHERITE_SHOVEL.getId());
    public static final SmithingUpgrade SPEAR = new SmithingUpgrade(Identifier.parse("smithing_netherite_spear"), ItemTypes.DIAMOND_SPEAR.getId(), ItemTypes.NETHERITE_SPEAR.getId());
    public static final SmithingUpgrade SWORD = new SmithingUpgrade(Identifier.parse("smithing_netherite_sword"), ItemTypes.DIAMOND_SWORD.getId(), ItemTypes.NETHERITE_SWORD.getId());
}
