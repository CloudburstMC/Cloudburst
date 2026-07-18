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

    public static final DamageType BLOCK_EXPLOSION = type("block_explosion", IS_EXPLOSION, NO_KNOCKBACK);
    public static final DamageType CONTACT = type("contact", NO_KNOCKBACK);
    public static final DamageType CUSTOM = type("custom", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType DROWNING = type("drowning", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType ENTITY_ATTACK = type("entity_attack", IS_ENTITY_ATTACK);
    public static final DamageType ENTITY_EXPLOSION = type("entity_explosion", IS_EXPLOSION, NO_KNOCKBACK);
    public static final DamageType FALL = type("fall", BYPASSES_ARMOR, IS_FALL, NO_KNOCKBACK);
    public static final DamageType FALLING_BLOCK = type("falling_block");
    public static final DamageType FIRE = type("fire", IS_FIRE, NO_KNOCKBACK);
    public static final DamageType FIRE_TICK = type("fire_tick", BYPASSES_ARMOR, IS_FIRE, NO_KNOCKBACK);
    public static final DamageType FREEZING = type("freezing", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType HUNGER = type("hunger", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType LAVA = type("lava", IS_FIRE, NO_KNOCKBACK);
    public static final DamageType LIGHTNING = type("lightning", NO_KNOCKBACK);
    public static final DamageType MAGIC = type("magic", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType PROJECTILE = type("projectile", IS_PROJECTILE);
    public static final DamageType SUFFOCATION = type("suffocation", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType SUICIDE = type("suicide", BYPASSES_ARMOR, NO_KNOCKBACK);
    public static final DamageType THORNS = type("thorns");
    public static final DamageType VOID = type("void", BYPASSES_ARMOR, NO_KNOCKBACK);

    private static DamageType type(String id, DamageTypeTag... tags) {
        return new DamageType(Identifier.parse(id), Set.of(tags));
    }
}
