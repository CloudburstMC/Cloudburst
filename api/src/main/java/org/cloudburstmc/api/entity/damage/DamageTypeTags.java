package org.cloudburstmc.api.entity.damage;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;

/**
 * Built-in damage type tags.
 */
@UtilityClass
public class DamageTypeTags {

    /**
     * Damage that always affects ender dragons.
     */
    public static final DamageTypeTag ALWAYS_HURTS_ENDER_DRAGONS = tag("always_hurts_ender_dragons");

    /**
     * Damage that always destroys armor stands.
     */
    public static final DamageTypeTag ALWAYS_KILLS_ARMOR_STANDS = tag("always_kills_armor_stands");

    /**
     * Falling damage that always becomes the significant fall cause.
     */
    public static final DamageTypeTag ALWAYS_MOST_SIGNIFICANT_FALL = tag("always_most_significant_fall");

    /**
     * Damage that always wakes hidden silverfish.
     */
    public static final DamageTypeTag ALWAYS_TRIGGERS_SILVERFISH = tag("always_triggers_silverfish");

    /**
     * Damage that does not trigger guardian thorns.
     */
    public static final DamageTypeTag AVOIDS_GUARDIAN_THORNS = tag("avoids_guardian_thorns");

    /**
     * Fire damage caused by standing on a block.
     */
    public static final DamageTypeTag BURN_FROM_STEPPING = tag("burn_from_stepping");

    /**
     * Damage that burns armor stands.
     */
    public static final DamageTypeTag BURNS_ARMOR_STANDS = tag("burns_armor_stands");

    /**
     * Damage that is not reduced by armor.
     */
    public static final DamageTypeTag BYPASSES_ARMOR = tag("bypasses_armor");

    /**
     * Damage that ignores the normal hurt cooldown.
     */
    public static final DamageTypeTag BYPASSES_COOLDOWN = tag("bypasses_cooldown");

    /**
     * Damage that is not reduced by status effects.
     */
    public static final DamageTypeTag BYPASSES_EFFECTS = tag("bypasses_effects");

    /**
     * Damage that is not reduced by enchantments.
     */
    public static final DamageTypeTag BYPASSES_ENCHANTMENTS = tag("bypasses_enchantments");

    /**
     * Damage that applies to otherwise invulnerable entities.
     */
    public static final DamageTypeTag BYPASSES_INVULNERABILITY = tag("bypasses_invulnerability");

    /**
     * Damage that is not reduced by resistance.
     */
    public static final DamageTypeTag BYPASSES_RESISTANCE = tag("bypasses_resistance");

    /**
     * Damage that cannot be blocked by a shield.
     */
    public static final DamageTypeTag BYPASSES_SHIELD = tag("bypasses_shield");

    /**
     * Damage that is not absorbed by wolf armor.
     */
    public static final DamageTypeTag BYPASSES_WOLF_ARMOR = tag("bypasses_wolf_armor");

    /**
     * Damage that can break an armor stand.
     */
    public static final DamageTypeTag CAN_BREAK_ARMOR_STAND = tag("can_break_armor_stand");

    /**
     * Damage that reduces helmet durability.
     */
    public static final DamageTypeTag DAMAGES_HELMET = tag("damages_helmet");

    /**
     * Damage that ignites armor stands.
     */
    public static final DamageTypeTag IGNITES_ARMOR_STANDS = tag("ignites_armor_stands");

    /**
     * Damage caused by drowning.
     */
    public static final DamageTypeTag IS_DROWNING = tag("is_drowning");

    /**
     * Damage caused by an explosion.
     */
    public static final DamageTypeTag IS_EXPLOSION = tag("is_explosion");

    /**
     * Damage caused by falling.
     */
    public static final DamageTypeTag IS_FALL = tag("is_fall");

    /**
     * Damage treated as fire damage.
     */
    public static final DamageTypeTag IS_FIRE = tag("is_fire");

    /**
     * Damage caused by freezing.
     */
    public static final DamageTypeTag IS_FREEZING = tag("is_freezing");

    /**
     * Damage caused by lightning.
     */
    public static final DamageTypeTag IS_LIGHTNING = tag("is_lightning");

    /**
     * Damage caused by a mace smash attack.
     */
    public static final DamageTypeTag IS_MACE_SMASH = tag("is_mace_smash");

    /**
     * Damage attributed to a player attack.
     */
    public static final DamageTypeTag IS_PLAYER_ATTACK = tag("is_player_attack");

    /**
     * Damage delivered by a projectile.
     */
    public static final DamageTypeTag IS_PROJECTILE = tag("is_projectile");

    /**
     * Damage that does not make entities angry at its source.
     */
    public static final DamageTypeTag NO_ANGER = tag("no_anger");

    /**
     * Damage that does not produce an impact reaction.
     */
    public static final DamageTypeTag NO_IMPACT = tag("no_impact");

    /**
     * Damage that does not apply normal attack knockback.
     */
    public static final DamageTypeTag NO_KNOCKBACK = tag("no_knockback");

    /**
     * Damage that causes mobs to panic.
     */
    public static final DamageTypeTag PANIC_CAUSES = tag("panic_causes");

    /**
     * Environmental damage that causes mobs to panic.
     */
    public static final DamageTypeTag PANIC_ENVIRONMENTAL_CAUSES = tag("panic_environmental_causes");

    /**
     * Damage ignored by sulfur cubes carrying a block.
     */
    public static final DamageTypeTag SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO = tag("sulfur_cube_with_block_immune_to");

    /**
     * Damage resisted by witches.
     */
    public static final DamageTypeTag WITCH_RESISTANT_TO = tag("witch_resistant_to");

    /**
     * Damage ignored by withers.
     */
    public static final DamageTypeTag WITHER_IMMUNE_TO = tag("wither_immune_to");

    private static DamageTypeTag tag(String id) {
        return DamageTypeTag.of(Identifier.parse(id));
    }
}
