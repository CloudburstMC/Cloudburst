package org.cloudburstmc.api.enchantment;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;

import java.lang.reflect.Field;
import java.util.*;

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
    public static final EnchantmentType AQUA_AFFINITY = type(Identifiers.AQUA_AFFINITY, 1, RARE, false, false, ARMOR_HEAD, null, exclusiveWith(), constant(1), constant(41), 4);
    public static final EnchantmentType BANE_OF_ARTHROPODS = type(Identifiers.BANE_OF_ARTHROPODS, 5, UNCOMMON, false, false, WEAPON, DAMAGE, exclusiveWith(DAMAGE), dynamic(5, 8), dynamic(25, 8), 2);
    public static final EnchantmentType BINDING = type(Identifiers.BINDING, 1, VERY_RARE, true, true, WEARABLE, null, exclusiveWith(), constant(25), constant(50), 8);
    public static final EnchantmentType BLAST_PROTECTION = type(Identifiers.BLAST_PROTECTION, 4, RARE, false, false, EnchantmentTarget.ARMOR, ARMOR, exclusiveWith(ARMOR), dynamic(5, 8), dynamic(13, 8), 4);
    public static final EnchantmentType BREACH = type(Identifiers.BREACH, 4, RARE, false, false, MACE, DAMAGE, exclusiveWith(DAMAGE), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType CHANNELING = type(Identifiers.CHANNELING, 1, VERY_RARE, false, false, TRIDENT, RIPTIDE_TRIDENT, exclusiveWith(), constant(25), constant(50), 8);
    public static final EnchantmentType DENSITY = type(Identifiers.DENSITY, 5, UNCOMMON, false, false, MACE, DAMAGE, exclusiveWith(DAMAGE), dynamic(5, 8), dynamic(25, 8), 2);
    public static final EnchantmentType DEPTH_STRIDER = type(Identifiers.DEPTH_STRIDER, 3, RARE, false, false, ARMOR_FEET, BOOTS, exclusiveWith(BOOTS), dynamic(10, 10), dynamic(25, 10), 4);
    public static final EnchantmentType EFFICIENCY = type(Identifiers.EFFICIENCY, 5, COMMON, false, false, TOOL, null, exclusiveWith(), dynamic(1, 10), dynamic(51, 10), 1);
    public static final EnchantmentType FEATHER_FALLING = type(Identifiers.FEATHER_FALLING, 4, UNCOMMON, false, false, ARMOR_FEET, null, exclusiveWith(), dynamic(5, 6), dynamic(11, 6), 2);
    public static final EnchantmentType FIRE_ASPECT = type(Identifiers.FIRE_ASPECT, 2, RARE, false, false, EnchantmentTarget.FIRE_ASPECT, null, exclusiveWith(), dynamic(10, 20), dynamic(60, 20), 4);
    public static final EnchantmentType FIRE_PROTECTION = type(Identifiers.FIRE_PROTECTION, 4, UNCOMMON, false, false, EnchantmentTarget.ARMOR, ARMOR, exclusiveWith(ARMOR), dynamic(10, 8), dynamic(18, 8), 2);
    public static final EnchantmentType FLAME = type(Identifiers.FLAME, 1, RARE, false, false, BOW, null, exclusiveWith(), constant(20), constant(50), 4);
    public static final EnchantmentType FORTUNE = type(Identifiers.FORTUNE, 3, RARE, false, false, TOOL, MINING, exclusiveWith(MINING), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType FROST_WALKER = type(Identifiers.FROST_WALKER, 2, RARE, true, false, ARMOR_FEET, BOOTS, exclusiveWith(BOOTS), dynamic(10, 10), dynamic(25, 10), 4);
    public static final EnchantmentType IMPALING = type(Identifiers.IMPALING, 5, RARE, false, false, TRIDENT, DAMAGE, exclusiveWith(DAMAGE), dynamic(1, 8), dynamic(21, 8), 4);
    public static final EnchantmentType INFINITY = type(Identifiers.INFINITY, 1, VERY_RARE, false, false, BOW, EnchantmentExclusiveGroup.BOW, exclusiveWith(EnchantmentExclusiveGroup.BOW), constant(20), constant(50), 8);
    public static final EnchantmentType KNOCKBACK = type(Identifiers.KNOCKBACK, 2, UNCOMMON, false, false, MELEE_WEAPON, null, exclusiveWith(), dynamic(5, 20), dynamic(55, 20), 2);
    public static final EnchantmentType LOOTING = type(Identifiers.LOOTING, 3, RARE, false, false, MELEE_WEAPON, null, exclusiveWith(), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType LOYALTY = type(Identifiers.LOYALTY, 3, UNCOMMON, false, false, TRIDENT, RIPTIDE_TRIDENT, exclusiveWith(), dynamic(12, 7), constant(50), 2);
    public static final EnchantmentType LUCK_OF_THE_SEA = type(Identifiers.LUCK_OF_THE_SEA, 3, RARE, false, false, FISHING_ROD, null, exclusiveWith(), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType LUNGE = type(Identifiers.LUNGE, 3, UNCOMMON, false, false, SPEAR, null, exclusiveWith(), dynamic(5, 8), dynamic(25, 8), 2);
    public static final EnchantmentType LURE = type(Identifiers.LURE, 3, RARE, false, false, FISHING_ROD, null, exclusiveWith(), dynamic(15, 9), dynamic(65, 9), 4);
    public static final EnchantmentType MENDING = type(Identifiers.MENDING, 1, RARE, true, false, BREAKABLE, EnchantmentExclusiveGroup.BOW, exclusiveWith(), dynamic(25, 25), dynamic(75, 25), 4);
    public static final EnchantmentType MULTISHOT = type(Identifiers.MULTISHOT, 1, RARE, false, false, CROSSBOW, CROSSBOW_PROJECTILE, exclusiveWith(CROSSBOW_PROJECTILE), constant(20), constant(50), 4);
    public static final EnchantmentType PIERCING = type(Identifiers.PIERCING, 4, COMMON, false, false, CROSSBOW, CROSSBOW_PROJECTILE, exclusiveWith(CROSSBOW_PROJECTILE), dynamic(1, 10), constant(50), 1);
    public static final EnchantmentType POWER = type(Identifiers.POWER, 5, COMMON, false, false, BOW, null, exclusiveWith(), dynamic(1, 10), dynamic(16, 10), 1);
    public static final EnchantmentType PROJECTILE_PROTECTION = type(Identifiers.PROJECTILE_PROTECTION, 4, UNCOMMON, false, false, EnchantmentTarget.ARMOR, ARMOR, exclusiveWith(ARMOR), dynamic(3, 6), dynamic(9, 6), 2);
    public static final EnchantmentType PROTECTION = type(Identifiers.PROTECTION, 4, COMMON, false, false, EnchantmentTarget.ARMOR, ARMOR, exclusiveWith(ARMOR), dynamic(1, 11), dynamic(12, 11), 1);
    public static final EnchantmentType PUNCH = type(Identifiers.PUNCH, 2, RARE, false, false, BOW, null, exclusiveWith(), dynamic(12, 20), dynamic(37, 20), 4);
    public static final EnchantmentType QUICK_CHARGE = type(Identifiers.QUICK_CHARGE, 3, UNCOMMON, false, false, CROSSBOW, null, exclusiveWith(), dynamic(12, 20), constant(50), 2);
    public static final EnchantmentType RESPIRATION = type(Identifiers.RESPIRATION, 3, RARE, false, false, ARMOR_HEAD, null, exclusiveWith(), dynamic(10, 10), dynamic(40, 10), 4);
    public static final EnchantmentType RIPTIDE = type(Identifiers.RIPTIDE, 3, RARE, false, false, TRIDENT, null, exclusiveWith(RIPTIDE_TRIDENT), dynamic(17, 7), constant(50), 4);
    public static final EnchantmentType SHARPNESS = type(Identifiers.SHARPNESS, 5, COMMON, false, false, SHARP_WEAPON, DAMAGE, exclusiveWith(DAMAGE), dynamic(1, 11), dynamic(21, 11), 1);
    public static final EnchantmentType SILK_TOUCH = type(Identifiers.SILK_TOUCH, 1, VERY_RARE, false, false, TOOL, MINING, exclusiveWith(MINING), constant(15), constant(65), 8);
    public static final EnchantmentType SMITE = type(Identifiers.SMITE, 5, UNCOMMON, false, false, WEAPON, DAMAGE, exclusiveWith(DAMAGE), dynamic(5, 8), dynamic(25, 8), 2);
    public static final EnchantmentType SOUL_SPEED = type(Identifiers.SOUL_SPEED, 3, VERY_RARE, true, false, ARMOR_FEET, null, exclusiveWith(), dynamic(10, 10), dynamic(25, 10), 8);
    public static final EnchantmentType SWIFT_SNEAK = type(Identifiers.SWIFT_SNEAK, 3, VERY_RARE, true, false, ARMOR_LEGS, null, exclusiveWith(), dynamic(25, 25), dynamic(75, 25), 8);
    public static final EnchantmentType THORNS = type(Identifiers.THORNS, 3, VERY_RARE, false, false, EnchantmentTarget.ARMOR, null, exclusiveWith(), dynamic(10, 20), dynamic(60, 20), 8);
    public static final EnchantmentType UNBREAKING = type(Identifiers.UNBREAKING, 3, UNCOMMON, false, false, BREAKABLE, null, exclusiveWith(), dynamic(5, 8), dynamic(55, 8), 2);
    public static final EnchantmentType VANISHING = type(Identifiers.VANISHING, 1, VERY_RARE, true, true, VANISHABLE, null, exclusiveWith(), constant(25), constant(50), 8);
    public static final EnchantmentType WIND_BURST = type(Identifiers.WIND_BURST, 3, RARE, true, false, MACE, null, exclusiveWith(), dynamic(15, 9), dynamic(65, 9), 4);

    private static Set<EnchantmentExclusiveGroup> exclusiveWith(EnchantmentExclusiveGroup... groups) {
        checkNotNull(groups, "groups");
        for (EnchantmentExclusiveGroup group : groups) {
            checkNotNull(group, "group");
        }
        return groups.length == 0 ? Set.of() : Set.of(groups);
    }

    private static EnchantmentType type(Identifier identifier, int maxLevel, EnchantmentRarity rarity,
                                        boolean treasure, boolean cursed, EnchantmentTarget target,
                                        @Nullable EnchantmentExclusiveGroup exclusiveGroup,
                                        Set<EnchantmentExclusiveGroup> exclusiveWithGroups, EnchantmentCost minCost,
                                        EnchantmentCost maxCost, int anvilCost) {
        return new EnchantmentType(identifier, maxLevel, rarity, treasure, cursed, target, exclusiveGroup,
                exclusiveWithGroups, minCost, maxCost, anvilCost);
    }

    public static Optional<EnchantmentType> get(Identifier id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(Lookup.VALUES.get(id));
    }

    /**
     * Returns all generated enchantment type constants in declaration order.
     *
     * @return generated enchantment type constants
     */
    public static Collection<EnchantmentType> values() {
        return Lookup.VALUES.values();
    }

    private static final class Lookup {
        private static final Map<Identifier, EnchantmentType> VALUES = create();

        private static Map<Identifier, EnchantmentType> create() {
            Map<Identifier, EnchantmentType> values = new LinkedHashMap<>();
            for (Field field : EnchantmentTypes.class.getFields()) {
                if (field.getType() != EnchantmentType.class) {
                    continue;
                }
                try {
                    EnchantmentType type = (EnchantmentType) field.get(null);
                    values.put(type.identifier(), type);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to read enchantment type field " + field.getName(), e);
                }
            }
            return Collections.unmodifiableMap(values);
        }
    }
}

