package org.cloudburstmc.api.potion;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifiers;

@UtilityClass
public class PotionTypes {
    public static final PotionType WATER = PotionType.builder().potionId(Identifiers.POTION_WATER).level(1).build();
    public static final PotionType MUNDANE = PotionType.builder().potionId(Identifiers.POTION_MUNDANE).level(1).build();
    public static final PotionType MUNDANE_II = PotionType.builder().potionId(Identifiers.POTION_MUNDANE_II).level(2).build();
    public static final PotionType THICK = PotionType.builder().potionId(Identifiers.POTION_THICK).level(1).build();
    public static final PotionType AWKWARD = PotionType.builder().potionId(Identifiers.POTION_AWKWARD).level(1).build();
    public static final PotionType NIGHT_VISION = PotionType.builder().potionId(Identifiers.POTION_NIGHT_VISION).type(EffectTypes.NIGHT_VISION).level(1).duration(180).build();
    public static final PotionType NIGHT_VISION_LONG = PotionType.builder().potionId(Identifiers.POTION_NIGHT_VISION_LONG).type(EffectTypes.NIGHT_VISION).level(1).duration(480).build();
    public static final PotionType INVISIBLE = PotionType.builder().potionId(Identifiers.POTION_INVISIBLE).type(EffectTypes.INVISIBILITY).level(1).duration(180).build();
    public static final PotionType INVISIBLE_LONG = PotionType.builder().potionId(Identifiers.POTION_INVISIBLE_LONG).type(EffectTypes.INVISIBILITY).level(1).duration(480).build();
    public static final PotionType LEAPING = PotionType.builder().potionId(Identifiers.POTION_LEAPING).type(EffectTypes.JUMP_BOOST).level(1).duration(180).build();
    public static final PotionType LEAPING_LONG = PotionType.builder().potionId(Identifiers.POTION_LEAPING_LONG).type(EffectTypes.JUMP_BOOST).level(1).duration(480).build();
    public static final PotionType LEAPING_II = PotionType.builder().potionId(Identifiers.POTION_LEAPING_II).type(EffectTypes.JUMP_BOOST).level(2).duration(90).build();
    public static final PotionType FIRE_RESISTANCE = PotionType.builder().potionId(Identifiers.POTION_FiRE_RESISTANCE).type(EffectTypes.FIRE_RESISTANCE).level(1).duration(180).build();
    public static final PotionType FIRE_RESISTANCE_LONG = PotionType.builder().potionId(Identifiers.POTION_FIRE_RESISTANCE_LONG).type(EffectTypes.FIRE_RESISTANCE).duration(480).level(1).build();
    public static final PotionType SPEED = PotionType.builder().potionId(Identifiers.POTION_SPEED).type(EffectTypes.SWIFTNESS).level(1).duration(180).build();
    public static final PotionType SPEED_LONG = PotionType.builder().potionId(Identifiers.POTION_SPEED_LONG).type(EffectTypes.SWIFTNESS).level(1).duration(480).build();
    public static final PotionType SPEED_II = PotionType.builder().potionId(Identifiers.POTION_SPEED_II).type(EffectTypes.SWIFTNESS).level(2).duration(480).build();
    public static final PotionType SLOWNESS = PotionType.builder().potionId(Identifiers.POTION_SLOWNESS).type(EffectTypes.SLOWNESS).level(1).duration(90).build();
    public static final PotionType SLOWNESS_LONG = PotionType.builder().potionId(Identifiers.POTION_SLOWNESS_LONG).type(EffectTypes.SLOWNESS).level(1).duration(240).build();
    public static final PotionType WATER_BREATHING = PotionType.builder().potionId(Identifiers.POTION_WATER_BREATHING).type(EffectTypes.WATER_BREATHING).level(1).duration(180).build();
    public static final PotionType WATER_BREATHING_LONG = PotionType.builder().potionId(Identifiers.POTION_WATER_BREATHING_LONG).type(EffectTypes.WATER_BREATHING).level(1).duration(480).build();
    public static final PotionType INSTANT_HEALTH = PotionType.builder().potionId(Identifiers.POTION_INSTANT_HEALTH).type(EffectTypes.HEALING).level(1).instant(true).build();
    public static final PotionType INSTANT_HEALTH_II = PotionType.builder().potionId(Identifiers.POTION_INSTANT_HEALTH_II).type(EffectTypes.HEALING).level(2).instant(true).build();
    public static final PotionType HARMING = PotionType.builder().potionId(Identifiers.POTION_HARMING).type(EffectTypes.HARMING).level(1).instant(true).build();
    public static final PotionType HARMING_II = PotionType.builder().potionId(Identifiers.POTION_HARMING_II).type(EffectTypes.HARMING).level(2).instant(true).build();
    public static final PotionType POISON = PotionType.builder().potionId(Identifiers.POTION_POISON).type(EffectTypes.POISON).level(1).duration(45).build();
    public static final PotionType POISON_LONG = PotionType.builder().potionId(Identifiers.POTION_POISON_LONG).type(EffectTypes.POISON).level(1).duration(120).build();
    public static final PotionType POISON_II = PotionType.builder().potionId(Identifiers.POTION_POISON_II).type(EffectTypes.POISON).level(2).duration(22).build();
    public static final PotionType REGENERATION = PotionType.builder().potionId(Identifiers.POTION_REGENERATION).type(EffectTypes.REGENERATION).level(1).duration(45).build();
    public static final PotionType REGENERATION_LONG = PotionType.builder().potionId(Identifiers.POTION_REGENERATION_LONG).type(EffectTypes.REGENERATION).level(1).duration(120).build();
    public static final PotionType REGENERATION_II = PotionType.builder().potionId(Identifiers.POTION_REGENERATION_II).type(EffectTypes.REGENERATION).level(2).duration(22).build();
    public static final PotionType STRENGTH = PotionType.builder().potionId(Identifiers.POTION_STRENGTH).type(EffectTypes.STRENGTH).level(1).duration(180).build();
    public static final PotionType STRENGTH_LONG = PotionType.builder().potionId(Identifiers.POTION_STRENGTH_LONG).type(EffectTypes.STRENGTH).level(1).duration(480).build();
    public static final PotionType STRENGTH_II = PotionType.builder().potionId(Identifiers.POTION_STRENGTH_II).type(EffectTypes.STRENGTH).level(2).duration(90).build();
    public static final PotionType WEAKNESS = PotionType.builder().potionId(Identifiers.POTION_WEAKNESS).type(EffectTypes.WEAKNESS).level(1).duration(90).build();
    public static final PotionType WEAKNESS_LONG = PotionType.builder().potionId(Identifiers.POTION_WEAKNESS_LONG).type(EffectTypes.WEAKNESS).level(1).duration(240).build();
    public static final PotionType DECAY = PotionType.builder().potionId(Identifiers.POTION_DECAY).type(EffectTypes.WITHER).level(2).duration(30).build();
    public static final PotionType TURTLE_MASTER = PotionType.builder().potionId(Identifiers.POTION_TURTLE_MASTER).type(EffectTypes.DAMAGE_RESISTANCE).level(1).duration(120).build(); // todo multiple effects
    public static final PotionType TURTLE_MASTER_LONG = PotionType.builder().potionId(Identifiers.POTION_TURTLE_MASTER_LONG).type(EffectTypes.DAMAGE_RESISTANCE).level(1).duration(480).build();
    public static final PotionType TURTLE_MASTER_II = PotionType.builder().potionId(Identifiers.POTION_TURTLE_MASTER_II).type(EffectTypes.DAMAGE_RESISTANCE).level(2).duration(90).build();
    public static final PotionType SLOW_FALLING = PotionType.builder().potionId(Identifiers.POTION_SLOW_FALLING).type(EffectTypes.SLOW_FALLING).level(1).duration(120).build();
    public static final PotionType SLOW_FALLING_LONG = PotionType.builder().potionId(Identifiers.POTION_SLOW_FALLING_LONG).type(EffectTypes.SLOW_FALLING).level(1).duration(480).build();
}
