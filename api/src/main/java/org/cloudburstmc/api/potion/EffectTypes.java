package org.cloudburstmc.api.potion;

import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.math.vector.Vector3i;

public class EffectTypes {

    public final static EffectType SWIFTNESS = EffectType.builder().id((byte) 1).type(Identifiers.SPEED).color(Vector3i.from(124, 175, 198)).build();
    public final static EffectType SLOWNESS = EffectType.builder().id((byte) 2).type(Identifiers.SLOWNESS).color(Vector3i.from(90, 108, 129)).bad(true).build();
    public final static EffectType HASTE = EffectType.builder().id((byte) 3).type(Identifiers.HASTE).color(Vector3i.from(217, 192, 67)).build();
    public final static EffectType MINING_FATIGUE = EffectType.builder().id((byte) 4).type(Identifiers.MINING_FATIGUE).color(Vector3i.from(74, 66, 23)).bad(true).build();
    public final static EffectType STRENGTH = EffectType.builder().id((byte) 5).type(Identifiers.STRENGTH).color(Vector3i.from(147, 36, 35)).build();
    public final static EffectType HEALING = EffectType.builder().id((byte) 6).type(Identifiers.HEALING).color(Vector3i.from(248, 36, 35)).build();
    public final static EffectType HARMING = EffectType.builder().id((byte) 7).type(Identifiers.HARMING).color(Vector3i.from(67, 10, 9)).bad(true).build();
    public final static EffectType JUMP_BOOST = EffectType.builder().id((byte) 8).type(Identifiers.JUMP_BOOST).color(Vector3i.from(34, 255, 76)).build();
    public final static EffectType NAUSEA = EffectType.builder().id((byte) 9).type(Identifiers.NAUSEA).color(Vector3i.from(85, 29, 74)).bad(true).build();
    public final static EffectType REGENERATION = EffectType.builder().id((byte) 10).type(Identifiers.REGENERATION).color(Vector3i.from(205, 92, 171)).build();
    public final static EffectType DAMAGE_RESISTANCE = EffectType.builder().id((byte) 11).type(Identifiers.DAMAGE_RESISTANCE).color(Vector3i.from(153, 69, 58)).build();
    public final static EffectType FIRE_RESISTANCE = EffectType.builder().id((byte) 12).type(Identifiers.FIRE_RESISTANCE).color(Vector3i.from(228, 157, 58)).build();
    public final static EffectType WATER_BREATHING = EffectType.builder().id((byte) 13).type(Identifiers.WATER_BREATHING).color(Vector3i.from(46, 82, 153)).build();
    public final static EffectType INVISIBILITY = EffectType.builder().id((byte) 14).type(Identifiers.INVISIBILITY).color(Vector3i.from(127, 131, 146)).build();
    public final static EffectType BLINDNESS = EffectType.builder().id((byte) 15).type(Identifiers.BLINDNESS).color(Vector3i.from(191, 192, 192)).build();
    public final static EffectType NIGHT_VISION = EffectType.builder().id((byte) 16).type(Identifiers.NIGHT_VISION).color(Vector3i.from(0, 0, 139)).build();
    public final static EffectType HUNGER = EffectType.builder().id((byte) 17).type(Identifiers.HUNGER).color(Vector3i.from(46, 139, 87)).build();
    public final static EffectType WEAKNESS = EffectType.builder().id((byte) 18).type(Identifiers.WEAKNESS).color(Vector3i.from(72, 77, 72)).bad(true).build();
    public final static EffectType POISON = EffectType.builder().id((byte) 19).type(Identifiers.POISON).color(Vector3i.from(78, 147, 49)).bad(true).build();
    public final static EffectType WITHER = EffectType.builder().id((byte) 20).type(Identifiers.WITHER).color(Vector3i.from(53, 42, 39)).bad(true).build();
    public final static EffectType HEALTH_BOOST = EffectType.builder().id((byte) 21).type(Identifiers.HEALTH_BOOST).color(Vector3i.from(248, 125, 35)).build();
    public final static EffectType ABSORPTION = EffectType.builder().id((byte) 22).type(Identifiers.ABSORPTION).color(Vector3i.from(36, 107, 251)).build();
    public final static EffectType SATURATION = EffectType.builder().id((byte) 23).type(Identifiers.SATURATION).color(Vector3i.from(255, 0, 255)).build();
    public final static EffectType LEVITATION = EffectType.builder().id((byte) 24).type(Identifiers.LEVITATION).color(Vector3i.from(206, 255, 255)).build();
    public final static EffectType FATAL_POISON = EffectType.builder().id((byte) 25).type(Identifiers.FATAL_POISON).color(Vector3i.from(78, 147, 49)).bad(true).build();
    public final static EffectType CONDUIT_POWER = EffectType.builder().id((byte) 26).type(Identifiers.CONDUIT_POWER).color(Vector3i.from(29, 194, 209)).build();
    public final static EffectType SLOW_FALLING = EffectType.builder().id((byte) 27).type(Identifiers.SLOW_FALLING).color(Vector3i.from(206, 255, 255)).build();
    public final static EffectType BAD_OMEN = EffectType.builder().id((byte) 28).type(Identifiers.BAD_OMEN).color(Vector3i.from(11, 97, 56)).build();
    public final static EffectType HERO_OF_THE_VILLAGE = EffectType.builder().id((byte) 29).type(Identifiers.HERO_OF_THE_VILLAGE).color(Vector3i.from(0, 0, 0)).build();

}
