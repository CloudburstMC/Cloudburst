package org.cloudburstmc.api.entity.damage;

/**
 * Selects how a death message is resolved for a damage type.
 */
public enum DeathMessageType {
    /**
     * Uses the damage type's standard death message.
     */
    DEFAULT,
    /**
     * Selects a contextual fall-damage message.
     */
    FALL_VARIANTS,
    /**
     * Uses the intentional-game-design message and link.
     */
    INTENTIONAL_GAME_DESIGN
}
