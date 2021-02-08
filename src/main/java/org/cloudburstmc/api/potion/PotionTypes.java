package org.cloudburstmc.api.potion;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PotionTypes {
    public static final PotionType WATER = PotionType.builder().networkId(0).level(1).build();
    public static final PotionType MUNDANE = PotionType.builder().networkId(1).level(1).build();
    public static final PotionType MUNDANE_II = PotionType.builder().networkId(2).level(2).build();
    public static final PotionType THICK = PotionType.builder().networkId(3).level(1).build();
    ;
    public static final PotionType AWKWARD = PotionType.builder().networkId(4).level(1).build();
    public static final PotionType NIGHT_VISION = PotionType.builder().networkId(5).type(EffectTypes.NIGHT_VISION).level(1).duration(180).build();
    public static final PotionType NIGHT_VISION_LONG = PotionType.builder().networkId(6).type(EffectTypes.NIGHT_VISION).level(1).duration(480).build();
    public static final PotionType INVISIBLE = PotionType.builder().networkId(7).type(EffectTypes.INVISIBILITY).level(1).duration(180).build();
    public static final PotionType INVISIBLE_LONG = PotionType.builder().networkId(8).type(EffectTypes.INVISIBILITY).level(1).duration(480).build();
    public static final PotionType LEAPING = PotionType.builder().networkId(9).type(EffectTypes.JUMP_BOOST).level(1).duration(180).build();
    public static final PotionType LEAPING_LONG = PotionType.builder().networkId(10).type(EffectTypes.JUMP_BOOST).level(1).duration(480).build();
    public static final PotionType LEAPING_II = PotionType.builder().networkId(11).type(EffectTypes.JUMP_BOOST).level(2).duration(90).build();
    public static final PotionType FIRE_RESISTANCE = PotionType.builder().networkId(12).type(EffectTypes.FIRE_RESISTANCE).level(1).duration(180).build();
    public static final PotionType FIRE_RESISTANCE_LONG = PotionType.builder().networkId(13).type(EffectTypes.FIRE_RESISTANCE).duration(480).level(1).build();
    public static final PotionType SPEED = PotionType.builder().networkId(14).type(EffectTypes.SWIFTNESS).level(1).duration(180).build();
    public static final PotionType SPEED_LONG = PotionType.builder().networkId(15).type(EffectTypes.SWIFTNESS).level(1).duration(480).build();
    public static final PotionType SPEED_II = PotionType.builder().networkId(16).type(EffectTypes.SWIFTNESS).level(2).duration(480).build();
    public static final PotionType SLOWNESS = PotionType.builder().networkId(17).type(EffectTypes.SLOWNESS).level(1).duration(90).build();
    public static final PotionType SLOWNESS_LONG = PotionType.builder().networkId(18).type(EffectTypes.SLOWNESS).level(1).duration(240).build();
    public static final PotionType WATER_BREATHING = PotionType.builder().networkId(19).type(EffectTypes.WATER_BREATHING).level(1).duration(180).build();
    public static final PotionType WATER_BREATHING_LONG = PotionType.builder().networkId(20).type(EffectTypes.WATER_BREATHING).level(1).duration(480).build();
    public static final PotionType INSTANT_HEALTH = PotionType.builder().networkId(21).type(EffectTypes.HEALING).level(1).instant(true).build();
    public static final PotionType INSTANT_HEALTH_II = PotionType.builder().networkId(22).type(EffectTypes.HEALING).level(2).instant(true).build();
    public static final PotionType HARMING = PotionType.builder().networkId(23).type(EffectTypes.HARMING).level(1).instant(true).build();
    public static final PotionType HARMING_II = PotionType.builder().networkId(24).type(EffectTypes.HARMING).level(2).instant(true).build();
    public static final PotionType POISON = PotionType.builder().networkId(25).type(EffectTypes.POISON).level(1).duration(45).build();
    public static final PotionType POISON_LONG = PotionType.builder().networkId(26).type(EffectTypes.POISON).level(1).duration(120).build();
    public static final PotionType POISON_II = PotionType.builder().networkId(27).type(EffectTypes.POISON).level(2).duration(22).build();
    public static final PotionType REGENERATION = PotionType.builder().networkId(28).type(EffectTypes.REGENERATION).level(1).duration(45).build();
    public static final PotionType REGENERATION_LONG = PotionType.builder().networkId(29).type(EffectTypes.REGENERATION).level(1).duration(120).build();
    public static final PotionType REGENERATION_II = PotionType.builder().networkId(30).type(EffectTypes.REGENERATION).level(2).duration(22).build();
    public static final PotionType STRENGTH = PotionType.builder().networkId(31).type(EffectTypes.STRENGTH).level(1).duration(180).build();
    public static final PotionType STRENGTH_LONG = PotionType.builder().networkId(32).type(EffectTypes.STRENGTH).level(1).duration(480).build();
    public static final PotionType STRENGTH_II = PotionType.builder().networkId(33).type(EffectTypes.STRENGTH).level(2).duration(90).build();
    public static final PotionType WEAKNESS = PotionType.builder().networkId(34).type(EffectTypes.WEAKNESS).level(1).duration(90).build();
    public static final PotionType WEAKNESS_LONG = PotionType.builder().networkId(35).type(EffectTypes.WEAKNESS).level(1).duration(240).build();
    public static final PotionType WITHER_II = PotionType.builder().networkId(36).type(EffectTypes.WITHER).level(2).duration(30).build();
    public static final PotionType TURTLE_MASTER = PotionType.builder().networkId(37).type(EffectTypes.DAMAGE_RESISTANCE).level(1).duration(120).build(); // todo multiple effects
    public static final PotionType TURTLE_MASTER_LONG = PotionType.builder().networkId(38).type(EffectTypes.DAMAGE_RESISTANCE).level(1).duration(480).build();
    public static final PotionType TURTLE_MASTER_II = PotionType.builder().networkId(39).type(EffectTypes.DAMAGE_RESISTANCE).level(2).duration(90).build();
    public static final PotionType SLOW_FALLING = PotionType.builder().networkId(40).type(EffectTypes.SLOW_FALLING).level(1).duration(120).build();
    public static final PotionType SLOW_FALLING_LONG = PotionType.builder().networkId(41).type(EffectTypes.SLOW_FALLING).level(1).duration(480).build();
}
