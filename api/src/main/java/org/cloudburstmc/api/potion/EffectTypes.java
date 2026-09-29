package org.cloudburstmc.api.potion;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.internal.BuiltInTypeCatalog;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class EffectTypes {
    private static final BuiltInTypeCatalog<EffectType> TYPES = BuiltInTypeCatalog.create(EffectType::getId);

    public static final EffectType ABSORPTION = beneficialType(Identifiers.ABSORPTION, Vector3i.from(37, 82, 165));
    public static final EffectType BAD_OMEN = neutralType(Identifiers.BAD_OMEN, Vector3i.from(11, 97, 56));
    public static final EffectType BLINDNESS = harmfulType(Identifiers.BLINDNESS, Vector3i.from(31, 31, 35));
    public static final EffectType BREATH_OF_THE_NAUTILUS = beneficialType(Identifiers.BREATH_OF_THE_NAUTILUS, Vector3i.from(0, 255, 238));
    public static final EffectType CONDUIT_POWER = beneficialType(Identifiers.CONDUIT_POWER, Vector3i.from(29, 194, 209));
    public static final EffectType DARKNESS = harmfulType(Identifiers.DARKNESS, Vector3i.from(41, 39, 33));
    public static final EffectType FATAL_POISON = harmfulType(Identifiers.FATAL_POISON, Vector3i.from(78, 147, 49));
    public static final EffectType FIRE_RESISTANCE = beneficialType(Identifiers.FIRE_RESISTANCE, Vector3i.from(228, 154, 58));
    public static final EffectType HASTE = beneficialType(Identifiers.HASTE, Vector3i.from(217, 192, 67));
    public static final EffectType HEALTH_BOOST = beneficialType(Identifiers.HEALTH_BOOST, Vector3i.from(248, 125, 35));
    public static final EffectType HUNGER = harmfulType(Identifiers.HUNGER, Vector3i.from(88, 118, 83));
    public static final EffectType INFESTED = harmfulType(Identifiers.INFESTED, Vector3i.from(140, 155, 140));
    public static final EffectType INSTANT_DAMAGE = harmfulType(Identifiers.INSTANT_DAMAGE, Vector3i.from(67, 10, 9));
    public static final EffectType INSTANT_HEALTH = beneficialType(Identifiers.INSTANT_HEALTH, Vector3i.from(248, 36, 35));
    public static final EffectType INVISIBILITY = beneficialType(Identifiers.INVISIBILITY, Vector3i.from(127, 131, 146));
    public static final EffectType JUMP_BOOST = beneficialType(Identifiers.JUMP_BOOST, Vector3i.from(34, 255, 76));
    public static final EffectType LEVITATION = harmfulType(Identifiers.LEVITATION, Vector3i.from(206, 255, 255));
    public static final EffectType MINING_FATIGUE = harmfulType(Identifiers.MINING_FATIGUE, Vector3i.from(74, 66, 23));
    public static final EffectType NAUSEA = harmfulType(Identifiers.NAUSEA, Vector3i.from(85, 29, 74));
    public static final EffectType NIGHT_VISION = beneficialType(Identifiers.NIGHT_VISION, Vector3i.from(31, 31, 161));
    public static final EffectType OOZING = harmfulType(Identifiers.OOZING, Vector3i.from(153, 255, 163));
    public static final EffectType POISON = harmfulType(Identifiers.POISON, Vector3i.from(78, 147, 49));
    public static final EffectType RAID_OMEN = neutralType(Identifiers.RAID_OMEN, Vector3i.from(222, 64, 88));
    public static final EffectType REGENERATION = beneficialType(Identifiers.REGENERATION, Vector3i.from(205, 92, 171));
    public static final EffectType RESISTANCE = beneficialType(Identifiers.RESISTANCE, Vector3i.from(153, 69, 58));
    public static final EffectType SATURATION = beneficialType(Identifiers.SATURATION, Vector3i.from(248, 36, 33));
    public static final EffectType SLOW_FALLING = beneficialType(Identifiers.SLOW_FALLING, Vector3i.from(247, 248, 224));
    public static final EffectType SLOWNESS = harmfulType(Identifiers.SLOWNESS, Vector3i.from(90, 108, 129));
    public static final EffectType SPEED = beneficialType(Identifiers.SPEED, Vector3i.from(124, 175, 198));
    public static final EffectType STRENGTH = beneficialType(Identifiers.STRENGTH, Vector3i.from(147, 36, 35));
    public static final EffectType TRIAL_OMEN = neutralType(Identifiers.TRIAL_OMEN, Vector3i.from(22, 166, 166));
    public static final EffectType VILLAGE_HERO = beneficialType(Identifiers.VILLAGE_HERO, Vector3i.from(0, 0, 0));
    public static final EffectType WATER_BREATHING = beneficialType(Identifiers.WATER_BREATHING, Vector3i.from(46, 82, 153));
    public static final EffectType WEAKNESS = harmfulType(Identifiers.WEAKNESS, Vector3i.from(72, 77, 72));
    public static final EffectType WEAVING = harmfulType(Identifiers.WEAVING, Vector3i.from(120, 105, 90));
    public static final EffectType WIND_CHARGED = harmfulType(Identifiers.WIND_CHARGED, Vector3i.from(189, 201, 255));
    public static final EffectType WITHER = harmfulType(Identifiers.WITHER, Vector3i.from(53, 42, 39));

    /**
     * Finds a built-in effect type by identifier.
     *
     * @param id effect identifier
     * @return matching built-in effect type, if present
     */
    public static Optional<EffectType> get(Identifier id) {
        return TYPES.get(id);
    }

    /**
     * Returns all built-in effect types in declaration order.
     *
     * @return built-in effect types
     */
    public static List<EffectType> values() {
        return TYPES.values();
    }

    private static EffectType beneficialType(Identifier id, Vector3i color) {
        return type(id, color, EffectCategory.BENEFICIAL);
    }

    private static EffectType harmfulType(Identifier id, Vector3i color) {
        return type(id, color, EffectCategory.HARMFUL);
    }

    private static EffectType neutralType(Identifier id, Vector3i color) {
        return type(id, color, EffectCategory.NEUTRAL);
    }

    private static EffectType type(Identifier id, Vector3i color, EffectCategory category) {
        return TYPES.register(EffectType.of(id, color, category));
    }
}
