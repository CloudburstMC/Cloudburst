package org.cloudburstmc.api.entity.damage;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;

import java.util.Set;

import static org.cloudburstmc.api.entity.damage.DamageTypeTags.*;

/**
 * Built-in damage types.
 */
@UtilityClass
public class DamageTypes {

    public static final DamageType ARROW = type("arrow", IS_PROJECTILE);
    public static final DamageType BAD_RESPAWN_POINT = type("bad_respawn_point", IS_EXPLOSION, NO_KNOCKBACK);
    public static final DamageType CACTUS = type("cactus", NO_KNOCKBACK);
    public static final DamageType CAMPFIRE = type("campfire", IS_FIRE, NO_KNOCKBACK);
    public static final DamageType DRAGON_BREATH = type("dragon_breath", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType DROWN = type("drown", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType DRY_OUT = type("dry_out", NO_KNOCKBACK);
    public static final DamageType ENDER_PEARL = type("ender_pearl", BYPASSES_ARMOR, IS_FALL, NO_KNOCKBACK);
    public static final DamageType EXPLOSION = type("explosion", IS_EXPLOSION, NO_KNOCKBACK);
    public static final DamageType FALL = type("fall", BYPASSES_ARMOR, IS_FALL, NO_KNOCKBACK);
    public static final DamageType FALLING_ANVIL = type("falling_anvil");
    public static final DamageType FALLING_BLOCK = type("falling_block");
    public static final DamageType FALLING_STALACTITE = type("falling_stalactite");
    public static final DamageType FIREBALL = type("fireball", IS_FIRE, IS_PROJECTILE);
    public static final DamageType FIREWORKS = type("fireworks", IS_EXPLOSION);
    public static final DamageType FLY_INTO_WALL = type("fly_into_wall", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType FREEZE = type("freeze", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType GENERIC = type("generic", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType GENERIC_KILL = type("generic_kill", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType HOT_FLOOR = type("hot_floor", IS_FIRE, NO_KNOCKBACK);
    public static final DamageType IN_FIRE = type("in_fire", IS_FIRE, NO_KNOCKBACK);
    public static final DamageType IN_WALL = type("in_wall", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType INDIRECT_MAGIC = type("indirect_magic", BYPASSES_ARMOR);
    public static final DamageType LAVA = type("lava", IS_FIRE, NO_KNOCKBACK);
    public static final DamageType LIGHTNING_BOLT = type("lightning_bolt", NO_KNOCKBACK);
    public static final DamageType MACE_SMASH = type("mace_smash", IS_ENTITY_ATTACK);
    public static final DamageType MAGIC = type("magic", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType MOB_ATTACK = type("mob_attack", IS_ENTITY_ATTACK);
    public static final DamageType MOB_ATTACK_NO_AGGRO = type("mob_attack_no_aggro", IS_ENTITY_ATTACK);
    public static final DamageType MOB_PROJECTILE = type("mob_projectile", IS_PROJECTILE);
    public static final DamageType ON_FIRE = type("on_fire", BYPASSES_ARMOR, IS_FIRE, NO_KNOCKBACK);
    public static final DamageType OUT_OF_WORLD = type("out_of_world", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType PLAYER_ATTACK = type("player_attack", IS_ENTITY_ATTACK);
    public static final DamageType PLAYER_EXPLOSION = type("player_explosion", IS_EXPLOSION, NO_KNOCKBACK);
    public static final DamageType SONIC_BOOM = type("sonic_boom", BYPASSES_ARMOR);
    public static final DamageType SPEAR = type("spear", IS_ENTITY_ATTACK, NO_KNOCKBACK);
    public static final DamageType SPIT = type("spit");
    public static final DamageType STALAGMITE = type("stalagmite", BYPASSES_ARMOR, IS_FALL, NO_KNOCKBACK);
    public static final DamageType STARVE = type("starve", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType STING = type("sting");
    public static final DamageType SULFUR_CUBE_HOT = type("sulfur_cube_hot", IS_FIRE, NO_KNOCKBACK);
    public static final DamageType SWEET_BERRY_BUSH = type("sweet_berry_bush", NO_KNOCKBACK);
    public static final DamageType THORNS = type("thorns");
    public static final DamageType THROWN = type("thrown", IS_PROJECTILE);
    public static final DamageType TRIDENT = type("trident", IS_PROJECTILE);
    public static final DamageType UNATTRIBUTED_FIREBALL = type("unattributed_fireball", IS_FIRE, IS_PROJECTILE);
    public static final DamageType WIND_CHARGE = type("wind_charge", IS_PROJECTILE);
    public static final DamageType WITHER = type("wither", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType WITHER_SKULL = type("wither_skull", IS_PROJECTILE);

    private static DamageType type(String id, DamageTypeTag... tags) {
        return new DamageType(Identifier.parse(id), Set.of(tags));
    }
}
