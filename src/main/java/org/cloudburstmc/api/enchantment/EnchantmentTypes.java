package org.cloudburstmc.api.enchantment;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifiers;

import static org.cloudburstmc.api.enchantment.EnchantmentRarity.*;
import static org.cloudburstmc.api.enchantment.EnchantmentTarget.*;

@UtilityClass
public class EnchantmentTypes {
    public static final EnchantmentType PROTECTION = EnchantmentType.builder().id((short) 0).type(Identifiers.PROTECTION).maxLevel(4).rarity(COMMON).target(ARMOR).build();
    public static final EnchantmentType FIRE_PROTECTION = EnchantmentType.builder().id((short) 1).type(Identifiers.FIRE_PROTECTION).maxLevel(4).rarity(UNCOMMON).target(ARMOR).build();
    public static final EnchantmentType FEATHER_FALLING = EnchantmentType.builder().id((short) 2).type(Identifiers.FEATHER_FALLING).maxLevel(4).rarity(UNCOMMON).target(ARMOR_FEET).build();
    public static final EnchantmentType BLAST_PROTECTION = EnchantmentType.builder().id((short) 3).type(Identifiers.BLAST_PROTECTION).maxLevel(4).rarity(RARE).target(ARMOR).build();
    public static final EnchantmentType PROJECTILE_PROTECTION = EnchantmentType.builder().id((short) 4).type(Identifiers.PROJECTILE_PROTECTION).maxLevel(4).rarity(UNCOMMON).target(ARMOR).build();
    public static final EnchantmentType THORNS = EnchantmentType.builder().id((short) 5).type(Identifiers.THORNS).maxLevel(3).rarity(VERY_RARE).target(ARMOR).build();
    public static final EnchantmentType RESPIRATION = EnchantmentType.builder().id((short) 6).type(Identifiers.RESPIRATION).maxLevel(3).rarity(RARE).target(ARMOR_HEAD).build();
    public static final EnchantmentType DEPTH_STRIDER = EnchantmentType.builder().id((short) 7).type(Identifiers.DEPTH_STRIDER).maxLevel(3).rarity(RARE).target(ARMOR_FEET).build();
    public static final EnchantmentType AQUA_AFFINITY = EnchantmentType.builder().id((short) 8).type(Identifiers.AQUA_AFFINITY).maxLevel(1).rarity(RARE).target(ARMOR_HEAD).build();
    public static final EnchantmentType SHARPNESS = EnchantmentType.builder().id((short) 9).type(Identifiers.SHARPNESS).maxLevel(5).rarity(COMMON).target(WEAPON).build();
    public static final EnchantmentType SMITE = EnchantmentType.builder().id((short) 10).type(Identifiers.SMITE).maxLevel(5).rarity(UNCOMMON).target(WEAPON).build();
    public static final EnchantmentType BANE_OF_ARTHROPODS = EnchantmentType.builder().id((short) 11).type(Identifiers.BANE_OF_ARTHROPODS).maxLevel(5).rarity(UNCOMMON).target(WEAPON).build();
    public static final EnchantmentType KNOCKBACK = EnchantmentType.builder().id((short) 12).type(Identifiers.KNOCKBACK).maxLevel(2).rarity(UNCOMMON).target(WEAPON).build();
    public static final EnchantmentType FIRE_ASPECT = EnchantmentType.builder().id((short) 13).type(Identifiers.FIRE_ASPECT).maxLevel(2).rarity(RARE).target(WEAPON).build();
    public static final EnchantmentType LOOTING = EnchantmentType.builder().id((short) 14).type(Identifiers.LOOTING).maxLevel(3).rarity(RARE).target(WEAPON).build();
    public static final EnchantmentType EFFICIENCY = EnchantmentType.builder().id((short) 15).type(Identifiers.EFFICIENCY).maxLevel(5).rarity(COMMON).target(TOOL).build();
    public static final EnchantmentType SILK_TOUCH = EnchantmentType.builder().id((short) 16).type(Identifiers.SILK_TOUCH).maxLevel(1).rarity(VERY_RARE).target(TOOL).build();
    public static final EnchantmentType UNBREAKING = EnchantmentType.builder().id((short) 17).type(Identifiers.UNBREAKING).maxLevel(3).rarity(UNCOMMON).target(BREAKABLE).build();
    public static final EnchantmentType FORTUNE = EnchantmentType.builder().id((short) 18).type(Identifiers.FORTUNE).maxLevel(3).rarity(RARE).target(TOOL).build();
    public static final EnchantmentType POWER = EnchantmentType.builder().id((short) 19).type(Identifiers.POWER).maxLevel(5).rarity(COMMON).target(BOW).build();
    public static final EnchantmentType PUNCH = EnchantmentType.builder().id((short) 20).type(Identifiers.PUNCH).maxLevel(2).rarity(RARE).target(BOW).build();
    public static final EnchantmentType FLAME = EnchantmentType.builder().id((short) 21).type(Identifiers.FLAME).maxLevel(1).rarity(RARE).target(BOW).build();
    public static final EnchantmentType INFINITY = EnchantmentType.builder().id((short) 22).type(Identifiers.INFINITY).maxLevel(1).rarity(VERY_RARE).target(BOW).build();
    public static final EnchantmentType LUCK_OF_THE_SEA = EnchantmentType.builder().id((short) 23).type(Identifiers.LUCK_OF_THE_SEA).maxLevel(3).rarity(RARE).target(FISHING_ROD).build();
    public static final EnchantmentType LURE = EnchantmentType.builder().id((short) 24).type(Identifiers.LURE).maxLevel(3).rarity(RARE).target(FISHING_ROD).build();
    public static final EnchantmentType FROST_WALKER = EnchantmentType.builder().id((short) 25).type(Identifiers.FROST_WALKER).maxLevel(2).rarity(RARE).treasure(true).target(ARMOR_FEET).build();
    public static final EnchantmentType MENDING = EnchantmentType.builder().id((short) 26).type(Identifiers.MENDING).maxLevel(1).rarity(RARE).treasure(true).target(BREAKABLE).build();
    public static final EnchantmentType BINDING = EnchantmentType.builder().id((short) 27).type(Identifiers.BINDING).maxLevel(1).rarity(VERY_RARE).treasure(true).cursed(true).target(WEARABLE).build();
    public static final EnchantmentType VANISHING = EnchantmentType.builder().id((short) 28).type(Identifiers.VANISHING).maxLevel(1).rarity(VERY_RARE).treasure(true).cursed(true).target(VANISHABLE).build();
    public static final EnchantmentType IMPALING = EnchantmentType.builder().id((short) 29).type(Identifiers.IMPALING).maxLevel(5).rarity(RARE).target(TRIDENT).build();
    public static final EnchantmentType RIPTIDE = EnchantmentType.builder().id((short) 30).type(Identifiers.RIPTIDE).maxLevel(3).rarity(RARE).target(TRIDENT).build();
    public static final EnchantmentType LOYALTY = EnchantmentType.builder().id((short) 31).type(Identifiers.LOYALTY).maxLevel(3).rarity(UNCOMMON).target(TRIDENT).build();
    public static final EnchantmentType CHANNELING = EnchantmentType.builder().id((short) 32).type(Identifiers.CHANNELING).maxLevel(1).rarity(VERY_RARE).target(TRIDENT).build();
    public static final EnchantmentType MULTISHOT = EnchantmentType.builder().id((short) 33).type(Identifiers.MULTISHOT).maxLevel(1).rarity(RARE).target(CROSSBOW).build();
    public static final EnchantmentType PIERCING = EnchantmentType.builder().id((short) 34).type(Identifiers.PIERCING).maxLevel(4).rarity(COMMON).target(CROSSBOW).build();
    public static final EnchantmentType QUICK_CHARGE = EnchantmentType.builder().id((short) 35).type(Identifiers.QUICK_CHARGE).maxLevel(3).rarity(UNCOMMON).target(CROSSBOW).build();
    public static final EnchantmentType SOUL_SPEED = EnchantmentType.builder().id((short) 36).type(Identifiers.SOUL_SPEED).maxLevel(3).rarity(VERY_RARE).treasure(true).target(ARMOR_FEET).build();
}
