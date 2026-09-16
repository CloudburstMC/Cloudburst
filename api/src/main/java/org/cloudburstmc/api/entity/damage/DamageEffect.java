package org.cloudburstmc.api.entity.damage;

/**
 * Selects the feedback used when an entity takes damage.
 */
public enum DamageEffect {
    /**
     * Standard hurt feedback.
     */
    HURT,
    /**
     * Thorns retaliation feedback.
     */
    THORNS,
    /**
     * Drowning feedback.
     */
    DROWNING,
    /**
     * Fire or lava feedback.
     */
    BURNING,
    /**
     * Poking feedback, such as from a berry bush.
     */
    POKING,
    /**
     * Freezing feedback.
     */
    FREEZING
}
