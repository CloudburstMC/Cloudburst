package org.cloudburstmc.api.potion;

import com.nukkitx.math.vector.Vector3i;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifiers;

@UtilityClass
public class EffectTypes {

    public final static EffectType SWIFTNESS = new EffectType(Identifiers.SPEED, (byte) 1, Vector3i.from(124, 175, 198));
    public final static EffectType SLOWNESS = new EffectType(Identifiers.SLOWNESS, (byte) 2, Vector3i.from(90, 108, 129), true);
    public final static EffectType HASTE = new EffectType(Identifiers.HASTE, (byte) 3, Vector3i.from(217, 192, 67));
    public final static EffectType MINING_FATIGUE = new EffectType(Identifiers.MINING_FATIGUE, (byte) 4, Vector3i.from(74, 66, 23), true);
    public final static EffectType STRENGTH = new EffectType(Identifiers.STRENGTH, (byte) 5, Vector3i.from(147, 36, 35));
    public final static EffectType HEALING = new EffectType(Identifiers.HEALING, (byte) 6, Vector3i.from(248, 36, 35));
    public final static EffectType HARMING = new EffectType(Identifiers.HARMING, (byte) 7, Vector3i.from(67, 10, 9), true);
    public final static EffectType JUMP_BOOST = new EffectType(Identifiers.JUMP_BOOST, (byte) 8, Vector3i.from(34, 255, 76));
    public final static EffectType NAUSEA = new EffectType(Identifiers.NAUSEA, (byte) 9, Vector3i.from(85, 29, 74), true);
    public final static EffectType REGENERATION = new EffectType(Identifiers.REGENERATION, (byte) 10, Vector3i.from(205, 92, 171));
    public final static EffectType DAMAGE_RESISTANCE = new EffectType(Identifiers.DAMAGE_RESISTANCE, (byte) 11, Vector3i.from(153, 69, 58));
    public final static EffectType FIRE_RESISTANCE = new EffectType(Identifiers.FIRE_RESISTANCE, (byte) 12, Vector3i.from(228, 157, 58));
    public final static EffectType WATER_BREATHING = new EffectType(Identifiers.WATER_BREATHING, (byte) 13, Vector3i.from(46, 82, 153));
    public final static EffectType INVISIBILITY = new EffectType(Identifiers.INVISIBILITY, (byte) 14, Vector3i.from(127, 131, 146));
    public final static EffectType BLINDNESS = new EffectType(Identifiers.BLINDNESS, (byte) 15, Vector3i.from(191, 192, 192));
    public final static EffectType NIGHT_VISION = new EffectType(Identifiers.NIGHT_VISION, (byte) 16, Vector3i.from(0, 0, 139));
    public final static EffectType HUNGER = new EffectType(Identifiers.HUNGER, (byte) 17, Vector3i.from(46, 139, 87));
    public final static EffectType WEAKNESS = new EffectType(Identifiers.WEAKNESS, (byte) 18, Vector3i.from(72, 77, 72), true);
    public final static EffectType POISON = new EffectType(Identifiers.POISON, (byte) 19, Vector3i.from(78, 147, 49), true);
    public final static EffectType WITHER = new EffectType(Identifiers.WITHER, (byte) 20, Vector3i.from(53, 42, 39), true);
    public final static EffectType HEALTH_BOOST = new EffectType(Identifiers.HEALTH_BOOST, (byte) 21, Vector3i.from(248, 125, 35));
    public final static EffectType ABSORPTION = new EffectType(Identifiers.ABSORPTION, (byte) 22, Vector3i.from(36, 107, 251));
    public final static EffectType SATURATION = new EffectType(Identifiers.SATURATION, (byte) 23, Vector3i.from(255, 0, 255));
    public final static EffectType LEVITATION = new EffectType(Identifiers.LEVITATION, (byte) 24, Vector3i.from(206, 255, 255));
    public final static EffectType FATAL_POISON = new EffectType(Identifiers.FATAL_POISON, (byte) 25, Vector3i.from(78, 147, 49), true);
    public final static EffectType CONDUIT_POWER = new EffectType(Identifiers.CONDUIT_POWER, (byte) 26, Vector3i.from(29, 194, 209));
    public final static EffectType SLOW_FALLING = new EffectType(Identifiers.SLOW_FALLING, (byte) 27, Vector3i.from(206, 255, 255));
    public final static EffectType BAD_OMEN = new EffectType(Identifiers.BAD_OMEN, (byte) 28, Vector3i.from(11, 97, 56));
    public final static EffectType HERO_OF_THE_VILLAGE = new EffectType(Identifiers.HERO_OF_THE_VILLAGE, (byte) 29, Vector3i.from(0, 0, 0));

}
