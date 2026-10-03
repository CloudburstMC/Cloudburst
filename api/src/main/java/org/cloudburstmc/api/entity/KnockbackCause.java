package org.cloudburstmc.api.entity;

/**
 * Describes the action producing knockback.
 */
public enum KnockbackCause {
    /**
     * Damage without an attacking entity.
     */
    DAMAGE,
    /**
     * An entity attack, including a projectile hit.
     */
    ENTITY_ATTACK,
    /**
     * An explosion.
     */
    EXPLOSION,
    /**
     * Recoil from hitting a raised shield.
     */
    SHIELD_BLOCK
}
