package org.cloudburstmc.api.entity.damage;

/**
 * Determines when damage is scaled by the level difficulty.
 */
public enum DamageScaling {
    /**
     * Damage is not scaled.
     */
    NEVER,
    /**
     * Damage is scaled when caused by a non-player living entity.
     */
    WHEN_CAUSED_BY_LIVING_NON_PLAYER,
    /**
     * Damage is always scaled.
     */
    ALWAYS
}
