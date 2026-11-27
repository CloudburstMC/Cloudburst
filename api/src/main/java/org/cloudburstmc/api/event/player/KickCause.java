package org.cloudburstmc.api.event.player;

/**
 * Why a player was kicked, independent of the message shown to the player.
 */
public enum KickCause {
    /**
     * A plugin kicked the player.
     */
    PLUGIN,
    /**
     * The player is not on the enforced whitelist.
     */
    WHITELIST,
    /**
     * The player is banned.
     */
    BANNED,
    /**
     * The player's address is banned.
     */
    IP_BANNED,
    /**
     * A command kicked the player.
     */
    KICKED,
    /**
     * The player flew while flight is not allowed.
     */
    FLYING_PLAYER,
    /**
     * The player was idle for longer than the idle timeout.
     */
    IDLING,
    /**
     * The player interacted with an entity that cannot be attacked or used.
     */
    INVALID_ENTITY_ATTACKED,
    /**
     * The player sent packets faster than the rate limit.
     */
    SPAM,
    /**
     * No more specific cause applies.
     */
    UNKNOWN
}
