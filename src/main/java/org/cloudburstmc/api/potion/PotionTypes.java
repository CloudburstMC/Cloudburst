package org.cloudburstmc.api.potion;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifiers;

@UtilityClass
public class PotionTypes {
    public static final PotionType WATER = PotionType.builder().potionId(Identifiers.POTION_WATER).networkId(0).level(1).build();
    public static final PotionType MUNDANE = PotionType.builder().potionId(Identifiers.POTION_MUNDANE).networkId(1).level(1).build();
    public static final PotionType MUNDANE_II = PotionType.builder().potionId(Identifiers.POTION_MUNDANE_II).networkId(2).level(2).build();
    public static final PotionType THICK = PotionType.builder().potionId(Identifiers.POTION_THICK).networkId(3).level(1).build();
    public static final PotionType AWKWARD = PotionType.builder().potionId(Identifiers.POTION_AWKWARD).networkId(4).level(1).build();
    public static final PotionType NIGHT_VISION = PotionType.builder().potionId(Identifiers.POTION_NIGHT_VISION).networkId(5).type(EffectTypes.NIGHT_VISION).level(1).duration(180).build();
    public static final PotionType NIGHT_VISION_LONG = PotionType.builder().potionId(Identifiers.POTION_NIGHT_VISION_LONG).networkId(6).type(EffectTypes.NIGHT_VISION).level(1).duration(480).build();
    public static final PotionType INVISIBLE = PotionType.builder().potionId(Identifiers.POTION_INVISIBLE).networkId(7).type(EffectTypes.INVISIBILITY).level(1).duration(180).build();
    public static final PotionType INVISIBLE_LONG = PotionType.builder().potionId(Identifiers.POTION_INVISIBLE_LONG).networkId(8).type(EffectTypes.INVISIBILITY).level(1).duration(480).build();
    public static final PotionType LEAPING = PotionType.builder().potionId(Identifiers.POTION_LEAPING).networkId(9).type(EffectTypes.JUMP_BOOST).level(1).duration(180).build();
    public static final PotionType LEAPING_LONG = PotionType.builder().potionId(Identifiers.POTION_LEAPING_LONG).networkId(10).type(EffectTypes.JUMP_BOOST).level(1).duration(480).build();
    public static final PotionType LEAPING_II = PotionType.builder().potionId(Identifiers.POTION_LEAPING_II).networkId(11).type(EffectTypes.JUMP_BOOST).level(2).duration(90).build();
    public static final PotionType FIRE_RESISTANCE = PotionType.builder().potionId(Identifiers.POTION_FiRE_RESISTANCE).networkId(12).type(EffectTypes.FIRE_RESISTANCE).level(1).duration(180).build();
    public static final PotionType FIRE_RESISTANCE_LONG = PotionType.builder().potionId(Identifiers.POTION_FIRE_RESISTANCE_LONG).networkId(13).type(EffectTypes.FIRE_RESISTANCE).duration(480).level(1).build();
    public static final PotionType SPEED = PotionType.builder().potionId(Identifiers.POTION_SPEED).networkId(14).type(EffectTypes.SWIFTNESS).level(1).duration(180).build();
    public static final PotionType SPEED_LONG = PotionType.builder().potionId(Identifiers.POTION_SPEED_LONG).networkId(15).type(EffectTypes.SWIFTNESS).level(1).duration(480).build();
    public static final PotionType SPEED_II = PotionType.builder().potionId(Identifiers.POTION_SPEED_II).networkId(16).type(EffectTypes.SWIFTNESS).level(2).duration(480).build();
    public static final PotionType SLOWNESS = PotionType.builder().potionId(Identifiers.POTION_SLOWNESS).networkId(17).type(EffectTypes.SLOWNESS).level(1).duration(90).build();
    public static final PotionType SLOWNESS_LONG = PotionType.builder().potionId(Identifiers.POTION_SLOWNESS_LONG).networkId(18).type(EffectTypes.SLOWNESS).level(1).duration(240).build();
    public static final PotionType WATER_BREATHING = PotionType.builder().potionId(Identifiers.POTION_WATER_BREATHING).networkId(19).type(EffectTypes.WATER_BREATHING).level(1).duration(180).build();
    public static final PotionType WATER_BREATHING_LONG = PotionType.builder().potionId(Identifiers.POTION_WATER_BREATHING_LONG).networkId(20).type(EffectTypes.WATER_BREATHING).level(1).duration(480).build();
    public static final PotionType INSTANT_HEALTH = PotionType.builder().potionId(Identifiers.POTION_INSTANT_HEALTH).networkId(21).type(EffectTypes.HEALING).level(1).instant(true).build();
    public static final PotionType INSTANT_HEALTH_II = PotionType.builder().potionId(Identifiers.POTION_INSTANT_HEALTH_II).networkId(22).type(EffectTypes.HEALING).level(2).instant(true).build();
    public static final PotionType HARMING = PotionType.builder().potionId(Identifiers.POTION_HARMING).networkId(23).type(EffectTypes.HARMING).level(1).instant(true).build();
    public static final PotionType HARMING_II = PotionType.builder().potionId(Identifiers.POTION_HARMING_II).networkId(24).type(EffectTypes.HARMING).level(2).instant(true).build();
    public static final PotionType POISON = PotionType.builder().potionId(Identifiers.POTION_POISON).networkId(25).type(EffectTypes.POISON).level(1).duration(45).build();
    public static final PotionType POISON_LONG = PotionType.builder().potionId(Identifiers.POTION_POISON_LONG).networkId(26).type(EffectTypes.POISON).level(1).duration(120).build();
    public static final PotionType POISON_II = PotionType.builder().potionId(Identifiers.POTION_POISON_II).networkId(27).type(EffectTypes.POISON).level(2).duration(22).build();
    public static final PotionType REGENERATION = PotionType.builder().potionId(Identifiers.POTION_REGENERATION).networkId(28).type(EffectTypes.REGENERATION).level(1).duration(45).build();
    public static final PotionType REGENERATION_LONG = PotionType.builder().potionId(Identifiers.POTION_REGENERATION_LONG).networkId(29).type(EffectTypes.REGENERATION).level(1).duration(120).build();
    public static final PotionType REGENERATION_II = PotionType.builder().potionId(Identifiers.POTION_REGENERATION_II).networkId(30).type(EffectTypes.REGENERATION).level(2).duration(22).build();
    public static final PotionType STRENGTH = PotionType.builder().potionId(Identifiers.POTION_STRENGTH).networkId(31).type(EffectTypes.STRENGTH).level(1).duration(180).build();
    public static final PotionType STRENGTH_LONG = PotionType.builder().potionId(Identifiers.POTION_STRENGTH_LONG).networkId(32).type(EffectTypes.STRENGTH).level(1).duration(480).build();
    public static final PotionType STRENGTH_II = PotionType.builder().potionId(Identifiers.POTION_STRENGTH_II).networkId(33).type(EffectTypes.STRENGTH).level(2).duration(90).build();
    public static final PotionType WEAKNESS = PotionType.builder().potionId(Identifiers.POTION_WEAKNESS).networkId(34).type(EffectTypes.WEAKNESS).level(1).duration(90).build();
    public static final PotionType WEAKNESS_LONG = PotionType.builder().potionId(Identifiers.POTION_WEAKNESS_LONG).networkId(35).type(EffectTypes.WEAKNESS).level(1).duration(240).build();
    public static final PotionType DECAY = PotionType.builder().potionId(Identifiers.POTION_DECAY).networkId(36).type(EffectTypes.WITHER).level(2).duration(30).build();
    public static final PotionType TURTLE_MASTER = PotionType.builder().potionId(Identifiers.POTION_TURTLE_MASTER).networkId(37).type(EffectTypes.DAMAGE_RESISTANCE).level(1).duration(120).build(); // todo multiple effects
    public static final PotionType TURTLE_MASTER_LONG = PotionType.builder().potionId(Identifiers.POTION_TURTLE_MASTER_LONG).networkId(38).type(EffectTypes.DAMAGE_RESISTANCE).level(1).duration(480).build();
    public static final PotionType TURTLE_MASTER_II = PotionType.builder().potionId(Identifiers.POTION_TURTLE_MASTER_II).networkId(39).type(EffectTypes.DAMAGE_RESISTANCE).level(2).duration(90).build();
    public static final PotionType SLOW_FALLING = PotionType.builder().potionId(Identifiers.POTION_SLOW_FALLING).networkId(40).type(EffectTypes.SLOW_FALLING).level(1).duration(120).build();
    public static final PotionType SLOW_FALLING_LONG = PotionType.builder().potionId(Identifiers.POTION_SLOW_FALLING_LONG).networkId(41).type(EffectTypes.SLOW_FALLING).level(1).duration(480).build();
}
