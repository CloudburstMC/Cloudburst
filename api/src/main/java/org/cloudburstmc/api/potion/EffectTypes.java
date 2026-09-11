package org.cloudburstmc.api.potion;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.math.vector.Vector3i;

import java.lang.reflect.Field;
import java.util.*;

@UtilityClass
public class EffectTypes {
    public static final EffectType ABSORPTION = new EffectType(Identifiers.ABSORPTION, Vector3i.from(37, 82, 165));
    public static final EffectType BAD_OMEN = new EffectType(Identifiers.BAD_OMEN, Vector3i.from(11, 97, 56));
    public static final EffectType BLINDNESS = new EffectType(Identifiers.BLINDNESS, Vector3i.from(31, 31, 35), true);
    public static final EffectType CONDUIT_POWER = new EffectType(Identifiers.CONDUIT_POWER, Vector3i.from(29, 194, 209));
    public static final EffectType DARKNESS = new EffectType(Identifiers.DARKNESS, Vector3i.from(41, 39, 33), true);
    public static final EffectType FATAL_POISON = new EffectType(Identifiers.FATAL_POISON, Vector3i.from(78, 147, 49), true);
    public static final EffectType FIRE_RESISTANCE = new EffectType(Identifiers.FIRE_RESISTANCE, Vector3i.from(228, 154, 58));
    public static final EffectType HASTE = new EffectType(Identifiers.HASTE, Vector3i.from(217, 192, 67));
    public static final EffectType HEALTH_BOOST = new EffectType(Identifiers.HEALTH_BOOST, Vector3i.from(248, 125, 35));
    public static final EffectType HUNGER = new EffectType(Identifiers.HUNGER, Vector3i.from(88, 118, 83), true);
    public static final EffectType INFESTATION = new EffectType(Identifiers.INFESTATION, Vector3i.from(99, 117, 105), true);
    public static final EffectType INSTANT_DAMAGE = new EffectType(Identifiers.INSTANT_DAMAGE, Vector3i.from(67, 10, 9), true);
    public static final EffectType INSTANT_HEALTH = new EffectType(Identifiers.INSTANT_HEALTH, Vector3i.from(248, 36, 35));
    public static final EffectType INVISIBILITY = new EffectType(Identifiers.INVISIBILITY, Vector3i.from(127, 131, 146));
    public static final EffectType JUMP_BOOST = new EffectType(Identifiers.JUMP_BOOST, Vector3i.from(34, 255, 76));
    public static final EffectType LEVITATION = new EffectType(Identifiers.LEVITATION, Vector3i.from(206, 255, 255), true);
    public static final EffectType MINING_FATIGUE = new EffectType(Identifiers.MINING_FATIGUE, Vector3i.from(74, 66, 23), true);
    public static final EffectType NAUSEA = new EffectType(Identifiers.NAUSEA, Vector3i.from(85, 29, 74), true);
    public static final EffectType NIGHT_VISION = new EffectType(Identifiers.NIGHT_VISION, Vector3i.from(31, 31, 161));
    public static final EffectType OOZING = new EffectType(Identifiers.OOZING, Vector3i.from(100, 145, 99), true);
    public static final EffectType POISON = new EffectType(Identifiers.POISON, Vector3i.from(78, 147, 49), true);
    public static final EffectType REGENERATION = new EffectType(Identifiers.REGENERATION, Vector3i.from(205, 92, 171));
    public static final EffectType RESISTANCE = new EffectType(Identifiers.RESISTANCE, Vector3i.from(153, 69, 58));
    public static final EffectType SATURATION = new EffectType(Identifiers.SATURATION, Vector3i.from(248, 36, 33));
    public static final EffectType SLOWNESS = new EffectType(Identifiers.SLOWNESS, Vector3i.from(90, 108, 129), true);
    public static final EffectType SLOW_FALLING = new EffectType(Identifiers.SLOW_FALLING, Vector3i.from(247, 248, 224));
    public static final EffectType SPEED = new EffectType(Identifiers.SPEED, Vector3i.from(124, 175, 198));
    public static final EffectType STRENGTH = new EffectType(Identifiers.STRENGTH, Vector3i.from(147, 36, 35));
    public static final EffectType TRIAL_OMEN = new EffectType(Identifiers.TRIAL_OMEN, Vector3i.from(22, 166, 166), true);
    public static final EffectType VILLAGE_HERO = new EffectType(Identifiers.VILLAGE_HERO, Vector3i.from(0, 0, 0));
    public static final EffectType WATER_BREATHING = new EffectType(Identifiers.WATER_BREATHING, Vector3i.from(46, 82, 153));
    public static final EffectType WEAKNESS = new EffectType(Identifiers.WEAKNESS, Vector3i.from(72, 77, 72), true);
    public static final EffectType WEAVING = new EffectType(Identifiers.WEAVING, Vector3i.from(183, 195, 221));
    public static final EffectType WIND_CHARGING = new EffectType(Identifiers.WIND_CHARGING, Vector3i.from(179, 205, 210));
    public static final EffectType WITHER = new EffectType(Identifiers.WITHER, Vector3i.from(53, 42, 39), true);

    public static Optional<EffectType> get(Identifier id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(Lookup.VALUES.get(id));
    }

    /**
     * Returns all known effect types.
     *
     * @return known effect types
     */
    public static Collection<EffectType> values() {
        return Lookup.VALUES.values();
    }

    private static final class Lookup {
        private static final Map<Identifier, EffectType> VALUES = create();

        private static Map<Identifier, EffectType> create() {
            Map<Identifier, EffectType> values = new LinkedHashMap<>();
            for (Field field : EffectTypes.class.getFields()) {
                if (field.getType() != EffectType.class) {
                    continue;
                }
                try {
                    EffectType type = (EffectType) field.get(null);
                    values.put(type.getId(), type);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to read effect type field " + field.getName(), e);
                }
            }
            return Collections.unmodifiableMap(values);
        }
    }
}
