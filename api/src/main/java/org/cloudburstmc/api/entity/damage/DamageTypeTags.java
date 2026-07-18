package org.cloudburstmc.api.entity.damage;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;

/**
 * Built-in damage type tags.
 */
@UtilityClass
public class DamageTypeTags {

    /**
     * Damage that is not reduced by armor.
     */
    public static final DamageTypeTag BYPASSES_ARMOR = tag("bypasses_armor");

    /**
     * Damage delivered by an entity attack.
     */
    public static final DamageTypeTag IS_ENTITY_ATTACK = tag("is_entity_attack");

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
     * Damage delivered by a projectile.
     */
    public static final DamageTypeTag IS_PROJECTILE = tag("is_projectile");

    /**
     * Damage that does not apply normal attack knockback.
     */
    public static final DamageTypeTag NO_KNOCKBACK = tag("no_knockback");

    private static DamageTypeTag tag(String id) {
        return new DamageTypeTag(Identifier.parse(id));
    }
}
