package org.cloudburstmc.api.entity.damage;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.internal.BuiltInTypeCatalog;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;

import static org.cloudburstmc.api.entity.damage.DamageEffect.*;
import static org.cloudburstmc.api.entity.damage.DamageScaling.*;
import static org.cloudburstmc.api.entity.damage.DamageTypeTags.*;
import static org.cloudburstmc.api.entity.damage.DeathMessageType.*;

@UtilityClass
public class DamageTypes {
    private static final BuiltInTypeCatalog<DamageType> TYPES = BuiltInTypeCatalog.create(DamageType::getId);

    public static final DamageType ARROW = type("arrow", "arrow", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, IS_PROJECTILE, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, ALWAYS_KILLS_ARMOR_STANDS, PANIC_CAUSES);
    public static final DamageType BAD_RESPAWN_POINT = type("bad_respawn_point", "explosion.by.bed", ALWAYS, 0.1f, HURT, INTENTIONAL_GAME_DESIGN, IS_EXPLOSION, NO_KNOCKBACK);
    public static final DamageType CACTUS = type("cactus", "cactus", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, BYPASSES_SHIELD, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_KNOCKBACK, PANIC_ENVIRONMENTAL_CAUSES);
    public static final DamageType CAMPFIRE = type("campfire", "inFire", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, BURNING, DEFAULT, BYPASSES_SHIELD, IS_FIRE, IGNITES_ARMOR_STANDS, NO_KNOCKBACK, BURN_FROM_STEPPING, PANIC_ENVIRONMENTAL_CAUSES);
    public static final DamageType DRAGON_BREATH = type("dragon_breath", "magic", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, NO_KNOCKBACK, PANIC_CAUSES);
    public static final DamageType DROWN = type("drown", "drown", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, DROWNING, DEFAULT, BYPASSES_ARMOR, IS_DROWNING, NO_IMPACT, WITHER_IMMUNE_TO, NO_KNOCKBACK, BYPASSES_WOLF_ARMOR);
    public static final DamageType DRY_OUT = type("dry_out", "dehydration", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, BYPASSES_SHIELD, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_KNOCKBACK, BYPASSES_WOLF_ARMOR);
    public static final DamageType ENDER_PEARL = type("ender_pearl", "fall", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, FALL_VARIANTS, BYPASSES_ARMOR, IS_FALL, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_KNOCKBACK);
    public static final DamageType EXPLOSION = type("explosion", "explosion", ALWAYS, 0.1f, HURT, DEFAULT, IS_EXPLOSION, NO_KNOCKBACK, PANIC_CAUSES);
    public static final DamageType FALL = type("fall", "fall", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, FALL_VARIANTS, BYPASSES_ARMOR, IS_FALL, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_KNOCKBACK);
    public static final DamageType FALLING_ANVIL = type("falling_anvil", "anvil", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, DAMAGES_HELMET, BYPASSES_SHIELD, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO);
    public static final DamageType FALLING_BLOCK = type("falling_block", "fallingBlock", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, DAMAGES_HELMET, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO);
    public static final DamageType FALLING_STALACTITE = type("falling_stalactite", "stalactite", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, DAMAGES_HELMET, BYPASSES_SHIELD, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO);
    public static final DamageType FIREBALL = type("fireball", "fireball", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, BURNING, DEFAULT, IS_FIRE, IS_PROJECTILE, ALWAYS_KILLS_ARMOR_STANDS, PANIC_CAUSES);
    public static final DamageType FIREWORKS = type("fireworks", "fireworks", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, IS_EXPLOSION, PANIC_CAUSES);
    public static final DamageType FLY_INTO_WALL = type("fly_into_wall", "flyIntoWall", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType FREEZE = type("freeze", "freeze", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, FREEZING, DEFAULT, BYPASSES_ARMOR, IS_FREEZING, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_KNOCKBACK, BYPASSES_WOLF_ARMOR, PANIC_ENVIRONMENTAL_CAUSES);
    public static final DamageType GENERIC = type("generic", "generic", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType GENERIC_KILL = type("generic_kill", "generic", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, BYPASSES_INVULNERABILITY, BYPASSES_RESISTANCE, NO_KNOCKBACK);
    public static final DamageType HOT_FLOOR = type("hot_floor", "magma", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, BURNING, DEFAULT, BYPASSES_SHIELD, IS_FIRE, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_KNOCKBACK, BURN_FROM_STEPPING, PANIC_ENVIRONMENTAL_CAUSES);
    public static final DamageType IN_FIRE = type("in_fire", "inFire", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, BURNING, DEFAULT, BYPASSES_SHIELD, IS_FIRE, IGNITES_ARMOR_STANDS, NO_KNOCKBACK, PANIC_ENVIRONMENTAL_CAUSES);
    public static final DamageType IN_WALL = type("in_wall", "inWall", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, NO_KNOCKBACK, BYPASSES_WOLF_ARMOR);
    public static final DamageType INDIRECT_MAGIC = type("indirect_magic", "indirectMagic", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, WITCH_RESISTANT_TO, BYPASSES_WOLF_ARMOR, PANIC_CAUSES);
    public static final DamageType LAVA = type("lava", "lava", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, BURNING, DEFAULT, BYPASSES_SHIELD, IS_FIRE, NO_KNOCKBACK, PANIC_ENVIRONMENTAL_CAUSES);
    public static final DamageType LIGHTNING_BOLT = type("lightning_bolt", "lightningBolt", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, BYPASSES_SHIELD, IS_LIGHTNING, NO_KNOCKBACK, PANIC_ENVIRONMENTAL_CAUSES);
    public static final DamageType MACE_SMASH = type("mace_smash", "maceSmash.player", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, IS_PLAYER_ATTACK, IS_MACE_SMASH);
    public static final DamageType MAGIC = type("magic", "magic", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, WITCH_RESISTANT_TO, AVOIDS_GUARDIAN_THORNS, ALWAYS_TRIGGERS_SILVERFISH, NO_KNOCKBACK, BYPASSES_WOLF_ARMOR, PANIC_CAUSES);
    public static final DamageType MOB_ATTACK = type("mob_attack", "mob", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, PANIC_CAUSES);
    public static final DamageType MOB_ATTACK_NO_AGGRO = type("mob_attack_no_aggro", "mob", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_ANGER);
    public static final DamageType MOB_PROJECTILE = type("mob_projectile", "mob", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, IS_PROJECTILE, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, PANIC_CAUSES);
    public static final DamageType ON_FIRE = type("on_fire", "onFire", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, BURNING, DEFAULT, BYPASSES_ARMOR, IS_FIRE, BURNS_ARMOR_STANDS, NO_KNOCKBACK, PANIC_ENVIRONMENTAL_CAUSES);
    public static final DamageType OUT_OF_WORLD = type("out_of_world", "outOfWorld", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, BYPASSES_INVULNERABILITY, BYPASSES_RESISTANCE, ALWAYS_MOST_SIGNIFICANT_FALL, NO_KNOCKBACK);
    public static final DamageType PLAYER_ATTACK = type("player_attack", "player", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, IS_PLAYER_ATTACK);
    public static final DamageType PLAYER_EXPLOSION = type("player_explosion", "explosion.player", ALWAYS, 0.1f, HURT, DEFAULT, IS_EXPLOSION, NO_KNOCKBACK, CAN_BREAK_ARMOR_STAND, PANIC_CAUSES);
    public static final DamageType SONIC_BOOM = type("sonic_boom", "sonicBoom", ALWAYS, 0, HURT, DEFAULT, BYPASSES_ARMOR, BYPASSES_ENCHANTMENTS, WITCH_RESISTANT_TO, PANIC_CAUSES);
    public static final DamageType SPEAR = type("spear", "trident", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, IS_PLAYER_ATTACK, NO_KNOCKBACK);
    public static final DamageType SPIT = type("spit", "mob", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO);
    public static final DamageType STALAGMITE = type("stalagmite", "stalagmite", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, IS_FALL, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_KNOCKBACK);
    public static final DamageType STARVE = type("starve", "starve", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, BYPASSES_EFFECTS, NO_KNOCKBACK, BYPASSES_WOLF_ARMOR);
    public static final DamageType STING = type("sting", "mob", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, PANIC_CAUSES);
    public static final DamageType SULFUR_CUBE_HOT = type("sulfur_cube_hot", "sulfurCube.hot", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, BURNING, DEFAULT, BYPASSES_SHIELD, IS_FIRE, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_KNOCKBACK, BURN_FROM_STEPPING, PANIC_ENVIRONMENTAL_CAUSES);
    public static final DamageType SWEET_BERRY_BUSH = type("sweet_berry_bush", "sweetBerry", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, POKING, DEFAULT, BYPASSES_SHIELD, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, NO_KNOCKBACK);
    public static final DamageType THORNS = type("thorns", "thorns", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, DamageEffect.THORNS, DEFAULT, WITCH_RESISTANT_TO, AVOIDS_GUARDIAN_THORNS, BYPASSES_WOLF_ARMOR);
    public static final DamageType THROWN = type("thrown", "thrown", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, IS_PROJECTILE, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, PANIC_CAUSES);
    public static final DamageType TRIDENT = type("trident", "trident", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, IS_PROJECTILE, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, ALWAYS_KILLS_ARMOR_STANDS, PANIC_CAUSES);
    public static final DamageType UNATTRIBUTED_FIREBALL = type("unattributed_fireball", "onFire", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, BURNING, DEFAULT, IS_FIRE, IS_PROJECTILE, PANIC_CAUSES);
    public static final DamageType WIND_CHARGE = type("wind_charge", "mob", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, IS_PROJECTILE, SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO, ALWAYS_KILLS_ARMOR_STANDS, PANIC_CAUSES);
    public static final DamageType WITHER = type("wither", "wither", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0, HURT, DEFAULT, BYPASSES_ARMOR, NO_KNOCKBACK, BYPASSES_WOLF_ARMOR, PANIC_CAUSES);
    public static final DamageType WITHER_SKULL = type("wither_skull", "wither", WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, HURT, DEFAULT, IS_PROJECTILE, ALWAYS_KILLS_ARMOR_STANDS, PANIC_CAUSES);

    /**
     * Finds a built-in damage type by identifier.
     *
     * @param id the damage type identifier
     * @return the matching damage type, if known
     */
    public static Optional<DamageType> get(Identifier id) {
        return TYPES.get(id);
    }

    /**
     * Returns all built-in damage types.
     *
     * @return built-in damage types
     */
    public static List<DamageType> values() {
        return TYPES.values();
    }

    private static DamageType type(String id, String translationKeySuffix, DamageScaling scaling, float exhaustion, DamageEffect effect, DeathMessageType deathMessageType, DamageTypeTag... tags) {
        return TYPES.register(new DamageType(Identifier.parse(id), "death.attack." + translationKeySuffix, scaling, exhaustion, effect, deathMessageType, expandTags(tags)));
    }

    private static Set<DamageTypeTag> expandTags(DamageTypeTag... directTags) {
        Set<DamageTypeTag> tags = new LinkedHashSet<>(Arrays.asList(directTags));
        if (tags.contains(BYPASSES_ARMOR)) {
            tags.add(BYPASSES_SHIELD);
        }

        if (tags.contains(BYPASSES_INVULNERABILITY)) {
            tags.add(BYPASSES_WOLF_ARMOR);
        }

        if (tags.contains(IS_EXPLOSION)) {
            tags.add(SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO);
            tags.add(AVOIDS_GUARDIAN_THORNS);
            tags.add(ALWAYS_HURTS_ENDER_DRAGONS);
        }

        if (tags.contains(PANIC_ENVIRONMENTAL_CAUSES)) {
            tags.add(PANIC_CAUSES);
        }

        if (tags.contains(IS_PLAYER_ATTACK)) {
            tags.add(CAN_BREAK_ARMOR_STAND);
            tags.add(PANIC_CAUSES);
        }

        return Set.copyOf(tags);
    }
}
