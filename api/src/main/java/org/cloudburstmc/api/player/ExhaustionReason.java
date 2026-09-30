package org.cloudburstmc.api.player;

/**
 * Identifies the action or effect responsible for a player's exhaustion increase.
 */
public enum ExhaustionReason {
    /**
     * Mining a block.
     */
    BLOCK_MINED,
    /**
     * The Hunger effect.
     */
    HUNGER_EFFECT,
    /**
     * Taking damage.
     */
    DAMAGED,
    /**
     * Attacking an entity.
     */
    ATTACK,
    /**
     * Jumping without sprinting.
     */
    JUMP,
    /**
     * Jumping while sprinting.
     */
    JUMP_SPRINT,
    /**
     * Swimming.
     */
    SWIM,
    /**
     * Moving with the player's eyes underwater without swimming.
     */
    WALK_UNDERWATER,
    /**
     * Moving in water with the player's eyes above the surface.
     */
    WALK_ON_WATER,
    /**
     * Sprinting on land.
     */
    SPRINT,
    /**
     * Walking without sprinting. Normally adds no exhaustion.
     */
    WALK,
    /**
     * Walking while sneaking. Normally adds no exhaustion.
     */
    CROUCH,
    /**
     * Natural health regeneration.
     */
    REGEN,
    /**
     * An enchantment effect.
     */
    ENCHANTMENT_EFFECT,
    /**
     * Another unclassified cause.
     */
    OTHER
}
