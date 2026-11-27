package org.cloudburstmc.api.event.player;

/**
 * An animation a player shows to other players.
 */
public enum PlayerAnimationType {
    /**
     * No animation.
     */
    NO_ACTION,
    /**
     * The player swings their arm.
     */
    SWING_ARM,
    /**
     * The player gets out of bed.
     */
    WAKE_UP,
    /**
     * Critical hit particles.
     */
    CRITICAL_HIT,
    /**
     * Enchanted critical hit particles.
     */
    MAGIC_CRITICAL_HIT
}
