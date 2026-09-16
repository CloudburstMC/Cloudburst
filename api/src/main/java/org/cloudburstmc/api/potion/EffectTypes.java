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

    public static final EffectType ABSORPTION = type(Identifiers.ABSORPTION, Vector3i.from(37, 82, 165));
    public static final EffectType BAD_OMEN = type(Identifiers.BAD_OMEN, Vector3i.from(11, 97, 56));
    public static final EffectType BLINDNESS = harmfulType(Identifiers.BLINDNESS, Vector3i.from(31, 31, 35));
    public static final EffectType CONDUIT_POWER = type(Identifiers.CONDUIT_POWER, Vector3i.from(29, 194, 209));
    public static final EffectType DARKNESS = harmfulType(Identifiers.DARKNESS, Vector3i.from(41, 39, 33));
    public static final EffectType FATAL_POISON = harmfulType(Identifiers.FATAL_POISON, Vector3i.from(78, 147, 49));
    public static final EffectType FIRE_RESISTANCE = type(Identifiers.FIRE_RESISTANCE, Vector3i.from(228, 154, 58));
    public static final EffectType HASTE = type(Identifiers.HASTE, Vector3i.from(217, 192, 67));
    public static final EffectType HEALTH_BOOST = type(Identifiers.HEALTH_BOOST, Vector3i.from(248, 125, 35));
    public static final EffectType HUNGER = harmfulType(Identifiers.HUNGER, Vector3i.from(88, 118, 83));
    public static final EffectType INFESTATION = harmfulType(Identifiers.INFESTATION, Vector3i.from(99, 117, 105));
    public static final EffectType INSTANT_DAMAGE = harmfulType(Identifiers.INSTANT_DAMAGE, Vector3i.from(67, 10, 9));
    public static final EffectType INSTANT_HEALTH = type(Identifiers.INSTANT_HEALTH, Vector3i.from(248, 36, 35));
    public static final EffectType INVISIBILITY = type(Identifiers.INVISIBILITY, Vector3i.from(127, 131, 146));
    public static final EffectType JUMP_BOOST = type(Identifiers.JUMP_BOOST, Vector3i.from(34, 255, 76));
    public static final EffectType LEVITATION = harmfulType(Identifiers.LEVITATION, Vector3i.from(206, 255, 255));
    public static final EffectType MINING_FATIGUE = harmfulType(Identifiers.MINING_FATIGUE, Vector3i.from(74, 66, 23));
    public static final EffectType NAUSEA = harmfulType(Identifiers.NAUSEA, Vector3i.from(85, 29, 74));
    public static final EffectType NIGHT_VISION = type(Identifiers.NIGHT_VISION, Vector3i.from(31, 31, 161));
    public static final EffectType OOZING = harmfulType(Identifiers.OOZING, Vector3i.from(100, 145, 99));
    public static final EffectType POISON = harmfulType(Identifiers.POISON, Vector3i.from(78, 147, 49));
    public static final EffectType REGENERATION = type(Identifiers.REGENERATION, Vector3i.from(205, 92, 171));
    public static final EffectType RESISTANCE = type(Identifiers.RESISTANCE, Vector3i.from(153, 69, 58));
    public static final EffectType SATURATION = type(Identifiers.SATURATION, Vector3i.from(248, 36, 33));
    public static final EffectType SLOWNESS = harmfulType(Identifiers.SLOWNESS, Vector3i.from(90, 108, 129));
    public static final EffectType SLOW_FALLING = type(Identifiers.SLOW_FALLING, Vector3i.from(247, 248, 224));
    public static final EffectType SPEED = type(Identifiers.SPEED, Vector3i.from(124, 175, 198));
    public static final EffectType STRENGTH = type(Identifiers.STRENGTH, Vector3i.from(147, 36, 35));
    public static final EffectType TRIAL_OMEN = harmfulType(Identifiers.TRIAL_OMEN, Vector3i.from(22, 166, 166));
    public static final EffectType VILLAGE_HERO = type(Identifiers.VILLAGE_HERO, Vector3i.from(0, 0, 0));
    public static final EffectType WATER_BREATHING = type(Identifiers.WATER_BREATHING, Vector3i.from(46, 82, 153));
    public static final EffectType WEAKNESS = harmfulType(Identifiers.WEAKNESS, Vector3i.from(72, 77, 72));
    public static final EffectType WEAVING = type(Identifiers.WEAVING, Vector3i.from(183, 195, 221));
    public static final EffectType WIND_CHARGING = type(Identifiers.WIND_CHARGING, Vector3i.from(179, 205, 210));
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

    private static EffectType type(Identifier id, Vector3i color) {
        return TYPES.register(new EffectType(id, color));
    }

    private static EffectType harmfulType(Identifier id, Vector3i color) {
        return TYPES.register(new EffectType(id, color, true));
    }
}
