package org.cloudburstmc.api.enchantment;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.api.enchantment.EnchantmentCost.constant;
import static org.cloudburstmc.api.enchantment.EnchantmentCost.dynamic;
import static org.cloudburstmc.api.enchantment.EnchantmentExclusiveGroup.*;
import static org.cloudburstmc.api.enchantment.EnchantmentExclusiveGroup.ARMOR;
import static org.cloudburstmc.api.enchantment.EnchantmentRarity.*;
import static org.cloudburstmc.api.enchantment.EnchantmentTarget.*;
import static org.cloudburstmc.api.enchantment.EnchantmentTarget.BOW;

@UtilityClass
public class EnchantmentTypes {
    public static final EnchantmentType PROTECTION = type((short) 0, Identifiers.PROTECTION, 4, COMMON, false, false, EnchantmentTarget.ARMOR, ARMOR, exclusiveWith(ARMOR), dynamic(1, 11), dynamic(12, 11), 1);
    public static final EnchantmentType FIRE_PROTECTION = type((short) 1, Identifiers.FIRE_PROTECTION, 4, UNCOMMON, false, false, EnchantmentTarget.ARMOR, ARMOR, exclusiveWith(ARMOR), dynamic(10, 8), dynamic(18, 8), 2);
    public static final EnchantmentType FEATHER_FALLING = type((short) 2, Identifiers.FEATHER_FALLING, 4, UNCOMMON, false, false, ARMOR_FEET, null, exclusiveWith(), dynamic(5, 6), dynamic(11, 6), 2);
    public static final EnchantmentType BLAST_PROTECTION = type((short) 3, Identifiers.BLAST_PROTECTION, 4, RARE, false, false, EnchantmentTarget.ARMOR, ARMOR, exclusiveWith(ARMOR), dynamic(5, 8), dynamic(13, 8), 4);
    public static final EnchantmentType PROJECTILE_PROTECTION = type((short) 4, Identifiers.PROJECTILE_PROTECTION, 4, UNCOMMON, false, false, EnchantmentTarget.ARMOR, ARMOR, exclusiveWith(ARMOR), dynamic(3, 6), dynamic(9, 6), 2);
    public static final EnchantmentType THORNS = type((short) 5, Identifiers.THORNS, 3, VERY_RARE, false, false, EnchantmentTarget.ARMOR, null, exclusiveWith(), dynamic(10, 20), dynamic(60, 20), 8);
    public static final EnchantmentType RESPIRATION = type((short) 6, Identifiers.RESPIRATION, 3, RARE, false, false, ARMOR_HEAD, null, exclusiveWith(), dynamic(10, 10), dynamic(40, 10), 4);
    public static final EnchantmentType DEPTH_STRIDER = type((short) 7, Identifiers.DEPTH_STRIDER, 3, RARE, false, false, ARMOR_FEET, BOOTS, exclusiveWith(BOOTS), dynamic(10, 10), dynamic(25, 10), 4);
    public static final EnchantmentType AQUA_AFFINITY = type((short) 8, Identifiers.AQUA_AFFINITY, 1, RARE, false, false, ARMOR_HEAD, null, exclusiveWith(), constant(1), constant(41), 4);
    public static final EnchantmentType SHARPNESS = type((short) 9, Identifiers.SHARPNESS, 5, COMMON, false, false, SHARP_WEAPON, DAMAGE, exclusiveWith(DAMAGE), dynamic(1, 11), dynamic(21, 11), 1);
    public static final EnchantmentType SMITE = type((short) 10, Identifiers.SMITE, 5, UNCOMMON, false, false, WEAPON, DAMAGE, exclusiveWith(DAMAGE), dynamic(5, 8), dynamic(25, 8), 2);
    public static final EnchantmentType BANE_OF_ARTHROPODS = type((short) 11, Identifiers.BANE_OF_ARTHROPODS, 5, UNCOMMON, false, false, WEAPON, DAMAGE, exclusiveWith(DAMAGE), dynamic(5, 8), dynamic(25, 8), 2);
    public static final EnchantmentType KNOCKBACK = type((short) 12, Identifiers.KNOCKBACK, 2, UNCOMMON, false, false, MELEE_WEAPON, null, exclusiveWith(), dynamic(5, 20), dynamic(55, 20), 2);
    public static final EnchantmentType FIRE_ASPECT = type((short) 13, Identifiers.FIRE_ASPECT, 2, RARE, false, false, EnchantmentTarget.FIRE_ASPECT, null, exclusiveWith(), dynamic(10, 20), dynamic(60, 20), 4);
    public static final EnchantmentType LOOTING = type((short) 14, Identifiers.LOOTING, 3, RARE, false, false, MELEE_WEAPON, null, exclusiveWith(), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType EFFICIENCY = type((short) 15, Identifiers.EFFICIENCY, 5, COMMON, false, false, TOOL, null, exclusiveWith(), dynamic(1, 10), dynamic(51, 10), 1);
    public static final EnchantmentType SILK_TOUCH = type((short) 16, Identifiers.SILK_TOUCH, 1, VERY_RARE, false, false, TOOL, MINING, exclusiveWith(MINING), constant(15), constant(65), 8);
    public static final EnchantmentType UNBREAKING = type((short) 17, Identifiers.UNBREAKING, 3, UNCOMMON, false, false, BREAKABLE, null, exclusiveWith(), dynamic(5, 8), dynamic(55, 8), 2);
    public static final EnchantmentType FORTUNE = type((short) 18, Identifiers.FORTUNE, 3, RARE, false, false, TOOL, MINING, exclusiveWith(MINING), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType POWER = type((short) 19, Identifiers.POWER, 5, COMMON, false, false, BOW, null, exclusiveWith(), dynamic(1, 10), dynamic(16, 10), 1);
    public static final EnchantmentType PUNCH = type((short) 20, Identifiers.PUNCH, 2, RARE, false, false, BOW, null, exclusiveWith(), dynamic(12, 20), dynamic(37, 20), 4);
    public static final EnchantmentType FLAME = type((short) 21, Identifiers.FLAME, 1, RARE, false, false, BOW, null, exclusiveWith(), constant(20), constant(50), 4);
    public static final EnchantmentType INFINITY = type((short) 22, Identifiers.INFINITY, 1, VERY_RARE, false, false, BOW, EnchantmentExclusiveGroup.BOW, exclusiveWith(EnchantmentExclusiveGroup.BOW), constant(20), constant(50), 8);
    public static final EnchantmentType LUCK_OF_THE_SEA = type((short) 23, Identifiers.LUCK_OF_THE_SEA, 3, RARE, false, false, FISHING_ROD, null, exclusiveWith(), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType LURE = type((short) 24, Identifiers.LURE, 3, RARE, false, false, FISHING_ROD, null, exclusiveWith(), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType FROST_WALKER = type((short) 25, Identifiers.FROST_WALKER, 2, RARE, true, false, ARMOR_FEET, BOOTS, exclusiveWith(BOOTS), dynamic(10, 10), dynamic(25, 10), 4);
    public static final EnchantmentType MENDING = type((short) 26, Identifiers.MENDING, 1, RARE, true, false, BREAKABLE, EnchantmentExclusiveGroup.BOW, exclusiveWith(), dynamic(25, 25), dynamic(75, 25), 4);
    public static final EnchantmentType BINDING = type((short) 27, Identifiers.BINDING, 1, VERY_RARE, true, true, WEARABLE, null, exclusiveWith(), constant(25), constant(50), 8);
    public static final EnchantmentType VANISHING = type((short) 28, Identifiers.VANISHING, 1, VERY_RARE, true, true, VANISHABLE, null, exclusiveWith(), constant(25), constant(50), 8);
    public static final EnchantmentType IMPALING = type((short) 29, Identifiers.IMPALING, 5, RARE, false, false, TRIDENT, DAMAGE, exclusiveWith(DAMAGE), dynamic(1, 8), dynamic(21, 8), 4);
    public static final EnchantmentType RIPTIDE = type((short) 30, Identifiers.RIPTIDE, 3, RARE, false, false, TRIDENT, null, exclusiveWith(RIPTIDE_TRIDENT), dynamic(17, 7), constant(50), 4);
    public static final EnchantmentType LOYALTY = type((short) 31, Identifiers.LOYALTY, 3, UNCOMMON, false, false, TRIDENT, RIPTIDE_TRIDENT, exclusiveWith(), dynamic(12, 7), constant(50), 2);
    public static final EnchantmentType CHANNELING = type((short) 32, Identifiers.CHANNELING, 1, VERY_RARE, false, false, TRIDENT, RIPTIDE_TRIDENT, exclusiveWith(), constant(25), constant(50), 8);
    public static final EnchantmentType MULTISHOT = type((short) 33, Identifiers.MULTISHOT, 1, RARE, false, false, CROSSBOW, CROSSBOW_PROJECTILE, exclusiveWith(CROSSBOW_PROJECTILE), constant(20), constant(50), 4);
    public static final EnchantmentType PIERCING = type((short) 34, Identifiers.PIERCING, 4, COMMON, false, false, CROSSBOW, CROSSBOW_PROJECTILE, exclusiveWith(CROSSBOW_PROJECTILE), dynamic(1, 10), constant(50), 1);
    public static final EnchantmentType QUICK_CHARGE = type((short) 35, Identifiers.QUICK_CHARGE, 3, UNCOMMON, false, false, CROSSBOW, null, exclusiveWith(), dynamic(12, 20), constant(50), 2);
    public static final EnchantmentType SOUL_SPEED = type((short) 36, Identifiers.SOUL_SPEED, 3, VERY_RARE, true, false, ARMOR_FEET, null, exclusiveWith(), dynamic(10, 10), dynamic(25, 10), 8);
    public static final EnchantmentType SWIFT_SNEAK = type((short) 37, Identifiers.SWIFT_SNEAK, 3, VERY_RARE, true, false, ARMOR_LEGS, null, exclusiveWith(), dynamic(25, 25), dynamic(75, 25), 8);
    public static final EnchantmentType WIND_BURST = type((short) 38, Identifiers.WIND_BURST, 3, RARE, true, false, MACE, null, exclusiveWith(), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType DENSITY = type((short) 39, Identifiers.DENSITY, 5, UNCOMMON, false, false, MACE, DAMAGE, exclusiveWith(DAMAGE), dynamic(5, 8), dynamic(25, 8), 2);
    public static final EnchantmentType BREACH = type((short) 40, Identifiers.BREACH, 4, RARE, false, false, MACE, DAMAGE, exclusiveWith(DAMAGE), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType LUNGE = type((short) 41, Identifiers.LUNGE, 3, UNCOMMON, false, false, SPEAR, null, exclusiveWith(), dynamic(5, 8), dynamic(25, 8), 2);

    private static Set<EnchantmentExclusiveGroup> exclusiveWith(EnchantmentExclusiveGroup... groups) {
        checkNotNull(groups, "groups");
        for (EnchantmentExclusiveGroup group : groups) {
            checkNotNull(group, "group");
        }
        return groups.length == 0 ? Set.of() : Set.of(groups);
    }

    private static EnchantmentType type(short id, Identifier identifier, int maxLevel, EnchantmentRarity rarity,
                                        boolean treasure, boolean cursed, EnchantmentTarget target,
                                        @Nullable EnchantmentExclusiveGroup exclusiveGroup,
                                        Set<EnchantmentExclusiveGroup> exclusiveWithGroups, EnchantmentCost minCost,
                                        EnchantmentCost maxCost, int anvilCost) {
        return new EnchantmentType(id, identifier, maxLevel, rarity, treasure, cursed, target, exclusiveGroup,
                exclusiveWithGroups, minCost, maxCost, anvilCost);
    }
}
