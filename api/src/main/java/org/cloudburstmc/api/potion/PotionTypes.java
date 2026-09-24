package org.cloudburstmc.api.potion;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.internal.BuiltInTypeCatalog;
import org.cloudburstmc.api.util.Identifier;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class PotionTypes {
    private static final BuiltInTypeCatalog<PotionType> TYPES = BuiltInTypeCatalog.create(PotionType::getId);

    public static final PotionType AWKWARD = type("awkward");
    public static final PotionType FIRE_RESISTANCE = type("fire_resistance", effect(EffectTypes.FIRE_RESISTANCE, 3600));
    public static final PotionType HARMING = type("harming", effect(EffectTypes.INSTANT_DAMAGE, 1));
    public static final PotionType HEALING = type("healing", effect(EffectTypes.INSTANT_HEALTH, 1));
    public static final PotionType INFESTED = type("infested", effect(EffectTypes.INFESTED, 3600));
    public static final PotionType INVISIBILITY = type("invisibility", effect(EffectTypes.INVISIBILITY, 3600));
    public static final PotionType LEAPING = type("leaping", effect(EffectTypes.JUMP_BOOST, 3600));
    public static final PotionType LONG_FIRE_RESISTANCE = type("long_fire_resistance", effect(EffectTypes.FIRE_RESISTANCE, 9600));
    public static final PotionType LONG_INVISIBILITY = type("long_invisibility", effect(EffectTypes.INVISIBILITY, 9600));
    public static final PotionType LONG_LEAPING = type("long_leaping", effect(EffectTypes.JUMP_BOOST, 9600));
    public static final PotionType LONG_MUNDANE = type("long_mundane");
    public static final PotionType LONG_NIGHT_VISION = type("long_nightvision", effect(EffectTypes.NIGHT_VISION, 9600));
    public static final PotionType LONG_POISON = type("long_poison", effect(EffectTypes.POISON, 1800));
    public static final PotionType LONG_REGENERATION = type("long_regeneration", effect(EffectTypes.REGENERATION, 1800));
    public static final PotionType LONG_SLOW_FALLING = type("long_slow_falling", effect(EffectTypes.SLOW_FALLING, 4800));
    public static final PotionType LONG_SLOWNESS = type("long_slowness", effect(EffectTypes.SLOWNESS, 4800));
    public static final PotionType LONG_STRENGTH = type("long_strength", effect(EffectTypes.STRENGTH, 9600));
    public static final PotionType LONG_SWIFTNESS = type("long_swiftness", effect(EffectTypes.SPEED, 9600));
    public static final PotionType LONG_TURTLE_MASTER = type("long_turtle_master", effect(EffectTypes.SLOWNESS, 800, 3), effect(EffectTypes.RESISTANCE, 800, 2));
    public static final PotionType LONG_WATER_BREATHING = type("long_water_breathing", effect(EffectTypes.WATER_BREATHING, 9600));
    public static final PotionType LONG_WEAKNESS = type("long_weakness", effect(EffectTypes.WEAKNESS, 4800));
    public static final PotionType MUNDANE = type("mundane");
    public static final PotionType NIGHT_VISION = type("nightvision", effect(EffectTypes.NIGHT_VISION, 3600));
    public static final PotionType OOZING = type("oozing", effect(EffectTypes.OOZING, 3600));
    public static final PotionType POISON = type("poison", effect(EffectTypes.POISON, 900));
    public static final PotionType REGENERATION = type("regeneration", effect(EffectTypes.REGENERATION, 900));
    public static final PotionType SLOW_FALLING = type("slow_falling", effect(EffectTypes.SLOW_FALLING, 1800));
    public static final PotionType SLOWNESS = type("slowness", effect(EffectTypes.SLOWNESS, 1800));
    public static final PotionType STRENGTH = type("strength", effect(EffectTypes.STRENGTH, 3600));
    public static final PotionType STRONG_HARMING = type("strong_harming", effect(EffectTypes.INSTANT_DAMAGE, 1, 1));
    public static final PotionType STRONG_HEALING = type("strong_healing", effect(EffectTypes.INSTANT_HEALTH, 1, 1));
    public static final PotionType STRONG_LEAPING = type("strong_leaping", effect(EffectTypes.JUMP_BOOST, 1800, 1));
    public static final PotionType STRONG_POISON = type("strong_poison", effect(EffectTypes.POISON, 432, 1));
    public static final PotionType STRONG_REGENERATION = type("strong_regeneration", effect(EffectTypes.REGENERATION, 450, 1));
    public static final PotionType STRONG_SLOWNESS = type("strong_slowness", effect(EffectTypes.SLOWNESS, 400, 3));
    public static final PotionType STRONG_STRENGTH = type("strong_strength", effect(EffectTypes.STRENGTH, 1800, 1));
    public static final PotionType STRONG_SWIFTNESS = type("strong_swiftness", effect(EffectTypes.SPEED, 1800, 1));
    public static final PotionType STRONG_TURTLE_MASTER = type("strong_turtle_master", effect(EffectTypes.SLOWNESS, 400, 5), effect(EffectTypes.RESISTANCE, 400, 3));
    public static final PotionType SWIFTNESS = type("swiftness", effect(EffectTypes.SPEED, 3600));
    public static final PotionType THICK = type("thick");
    public static final PotionType TURTLE_MASTER = type("turtle_master", effect(EffectTypes.SLOWNESS, 400, 3), effect(EffectTypes.RESISTANCE, 400, 2));
    public static final PotionType WATER = type("water");
    public static final PotionType WATER_BREATHING = type("water_breathing", effect(EffectTypes.WATER_BREATHING, 3600));
    public static final PotionType WEAKNESS = type("weakness", effect(EffectTypes.WEAKNESS, 1800));
    public static final PotionType WEAVING = type("weaving", effect(EffectTypes.WEAVING, 3600));
    public static final PotionType WIND_CHARGED = type("wind_charged", effect(EffectTypes.WIND_CHARGED, 3600));
    public static final PotionType WITHER = type("wither", effect(EffectTypes.WITHER, 800));

    /**
     * Finds a built-in potion type by identifier.
     *
     * @param id potion identifier
     * @return matching built-in potion type, if present
     */
    public static Optional<PotionType> get(Identifier id) {
        return TYPES.get(id);
    }

    /**
     * Returns all built-in potion types in declaration order.
     *
     * @return built-in potion types
     */
    public static List<PotionType> values() {
        return TYPES.values();
    }

    private static PotionType type(String id, PotionEffect... effects) {
        return TYPES.register(PotionType.of(Identifier.parse(id), effects));
    }

    private static PotionEffect effect(EffectType type, int duration) {
        return effect(type, duration, 0);
    }

    private static PotionEffect effect(EffectType type, int duration, int amplifier) {
        return new PotionEffect(type, duration, amplifier);
    }
}
