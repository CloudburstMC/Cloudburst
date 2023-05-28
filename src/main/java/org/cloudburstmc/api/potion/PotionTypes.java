package org.cloudburstmc.api.potion;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifiers;

@UtilityClass
public class PotionTypes {
    public static final PotionType AWKWARD = PotionType.builder().potionId(Identifiers.POTION_AWKWARD).level(1).build();
    public static final PotionType FIRE_RESISTANCE = PotionType.builder().potionId(Identifiers.POTION_FiRE_RESISTANCE).type(EffectTypes.FIRE_RESISTANCE).level(1).duration(180).build();
    public static final PotionType HARMING = PotionType.builder().potionId(Identifiers.POTION_HARMING).type(EffectTypes.INSTANT_DAMAGE).level(1).instant(true).build();
    public static final PotionType HEALING = PotionType.builder().potionId(Identifiers.POTION_HEALING).type(EffectTypes.INSTANT_HEALTH).level(1).instant(true).build();
    public static final PotionType INVISIBILITY = PotionType.builder().potionId(Identifiers.POTION_INVISIBILITY).type(EffectTypes.INVISIBILITY).level(1).duration(180).build();
    public static final PotionType LEAPING = PotionType.builder().potionId(Identifiers.POTION_LEAPING).type(EffectTypes.JUMP_BOOST).level(1).duration(180).build();
    public static final PotionType LONG_FIRE_RESISTANCE = PotionType.builder().potionId(Identifiers.POTION_LONG_FIRE_RESISTANCE).type(EffectTypes.FIRE_RESISTANCE).duration(480).level(1).build();
    public static final PotionType LONG_INVISIBILITY = PotionType.builder().potionId(Identifiers.POTION_LONG_INVISIBILITY).type(EffectTypes.INVISIBILITY).level(1).duration(480).build();
    public static final PotionType LONG_LEAPING = PotionType.builder().potionId(Identifiers.POTION_LONG_LEAPING).type(EffectTypes.JUMP_BOOST).level(1).duration(480).build();
    public static final PotionType LONG_MUNDANE = PotionType.builder().potionId(Identifiers.POTION_LONG_MUNDANE).level(1).build();
    public static final PotionType LONG_NIGHT_VISION = PotionType.builder().potionId(Identifiers.POTION_LONG_NIGHT_VISION).type(EffectTypes.NIGHT_VISION).level(1).duration(480).build();
    public static final PotionType LONG_POISON = PotionType.builder().potionId(Identifiers.POTION_LONG_POISON).type(EffectTypes.POISON).level(1).duration(120).build();
    public static final PotionType LONG_REGENERATION = PotionType.builder().potionId(Identifiers.POTION_LONG_REGENERATION).type(EffectTypes.REGENERATION).level(1).duration(120).build();
    public static final PotionType LONG_SLOW_FALLING = PotionType.builder().potionId(Identifiers.POTION_LONG_SLOW_FALLING).type(EffectTypes.SLOW_FALLING).level(1).duration(240).build();
    public static final PotionType LONG_SLOWNESS = PotionType.builder().potionId(Identifiers.POTION_LONG_SLOWNESS).type(EffectTypes.SLOWNESS).level(1).duration(240).build();
    public static final PotionType LONG_STRENGTH = PotionType.builder().potionId(Identifiers.POTION_LONG_STRENGTH).type(EffectTypes.STRENGTH).level(1).duration(480).build();
    public static final PotionType LONG_SWIFTNESS = PotionType.builder().potionId(Identifiers.POTION_LONG_SWIFTNESS).type(EffectTypes.SPEED).level(1).duration(480).build();
    public static final PotionType LONG_TURTLE_MASTER = PotionType.builder().potionId(Identifiers.POTION_LONG_TURTLE_MASTER).type(EffectTypes.RESISTANCE).level(1).duration(480).build(); // TODO: Multiple effects
    public static final PotionType LONG_WATER_BREATHING = PotionType.builder().potionId(Identifiers.POTION_LONG_WATER_BREATHING).type(EffectTypes.WATER_BREATHING).level(1).duration(480).build();
    public static final PotionType LONG_WEAKNESS = PotionType.builder().potionId(Identifiers.POTION_LONG_WEAKNESS).type(EffectTypes.WEAKNESS).level(1).duration(240).build();
    public static final PotionType MUNDANE = PotionType.builder().potionId(Identifiers.POTION_MUNDANE).level(1).build();
    public static final PotionType NIGHT_VISION = PotionType.builder().potionId(Identifiers.POTION_NIGHT_VISION).type(EffectTypes.NIGHT_VISION).level(1).duration(180).build();
    public static final PotionType POISON = PotionType.builder().potionId(Identifiers.POTION_POISON).type(EffectTypes.POISON).level(1).duration(45).build();
    public static final PotionType REGENERATION = PotionType.builder().potionId(Identifiers.POTION_REGENERATION).type(EffectTypes.REGENERATION).level(1).duration(45).build();
    public static final PotionType SLOW_FALLING = PotionType.builder().potionId(Identifiers.POTION_SLOW_FALLING).type(EffectTypes.SLOW_FALLING).level(1).duration(90).build();
    public static final PotionType SLOWNESS = PotionType.builder().potionId(Identifiers.POTION_SLOWNESS).type(EffectTypes.SLOWNESS).level(1).duration(90).build();
    public static final PotionType STRENGTH = PotionType.builder().potionId(Identifiers.POTION_STRENGTH).type(EffectTypes.STRENGTH).level(1).duration(180).build();
    public static final PotionType STRONG_HARMING = PotionType.builder().potionId(Identifiers.POTION_STRONG_HARMING).type(EffectTypes.INSTANT_DAMAGE).level(2).instant(true).build();
    public static final PotionType STRONG_HEALING = PotionType.builder().potionId(Identifiers.POTION_STRONG_HEALING).type(EffectTypes.INSTANT_HEALTH).level(2).instant(true).build();
    public static final PotionType STRONG_LEAPING = PotionType.builder().potionId(Identifiers.POTION_STRONG_LEAPING).type(EffectTypes.JUMP_BOOST).level(2).duration(90).build();
    public static final PotionType STRONG_POISON = PotionType.builder().potionId(Identifiers.POTION_STRONG_POISON).type(EffectTypes.POISON).level(2).duration(22).build();
    public static final PotionType STRONG_REGENERATION = PotionType.builder().potionId(Identifiers.POTION_STRONG_REGENERATION).type(EffectTypes.REGENERATION).level(2).duration(22).build();
    public static final PotionType STRONG_SLOWNESS = PotionType.builder().potionId(Identifiers.POTION_STRONG_SLOWNESS).type(EffectTypes.SLOWNESS).level(2).duration(20).build();
    public static final PotionType STRONG_STRENGTH = PotionType.builder().potionId(Identifiers.POTION_STRONG_STRENGTH).type(EffectTypes.STRENGTH).level(2).duration(90).build();
    public static final PotionType STRONG_SWIFTNESS = PotionType.builder().potionId(Identifiers.POTION_STRONG_SWIFTNESS).type(EffectTypes.SPEED).level(2).duration(90).build();
    public static final PotionType STRONG_TURTLE_MASTER = PotionType.builder().potionId(Identifiers.POTION_STRONG_TURTLE_MASTER).type(EffectTypes.RESISTANCE).level(2).duration(90).build(); // TODO: Multiple effects
    public static final PotionType SWIFTNESS = PotionType.builder().potionId(Identifiers.POTION_SWIFTNESS).type(EffectTypes.SPEED).level(1).duration(180).build();
    public static final PotionType THICK = PotionType.builder().potionId(Identifiers.POTION_THICK).level(1).build();
    public static final PotionType TURTLE_MASTER = PotionType.builder().potionId(Identifiers.POTION_TURTLE_MASTER).type(EffectTypes.RESISTANCE).level(1).duration(120).build(); // TODO: Multiple effects
    public static final PotionType WATER = PotionType.builder().potionId(Identifiers.POTION_WATER).level(1).build();
    public static final PotionType WATER_BREATHING = PotionType.builder().potionId(Identifiers.POTION_WATER_BREATHING).type(EffectTypes.WATER_BREATHING).level(1).duration(180).build();
    public static final PotionType WEAKNESS = PotionType.builder().potionId(Identifiers.POTION_WEAKNESS).type(EffectTypes.WEAKNESS).level(1).duration(90).build();
    public static final PotionType WITHER = PotionType.builder().potionId(Identifiers.POTION_WITHER).type(EffectTypes.WITHER).level(1).duration(40).build();
}
