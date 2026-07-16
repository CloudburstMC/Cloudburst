package org.cloudburstmc.api.event.player;

/**
 * An action in a player's fishing lifecycle.
 */
public enum PlayerFishState {
    /**
     * The player cast a new fishing hook.
     */
    CAST,
    /**
     * A catch began approaching the hook.
     */
    LURED,
    /**
     * A catch bit and is ready to be retrieved.
     */
    BITE,
    /**
     * The player retrieved an item as fishing loot.
     */
    CAUGHT_ITEM,
    /**
     * The player retrieved an entity attached to the hook.
     */
    CAUGHT_ENTITY,
    /**
     * The player retrieved a hook resting against a block.
     */
    IN_GROUND,
    /**
     * The bite expired before the player retrieved the hook.
     */
    FAILED_ATTEMPT,
    /**
     * The player retrieved the hook without catching anything.
     */
    REEL_IN
}
