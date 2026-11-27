package org.cloudburstmc.api.event.player;

/**
 * How a player's connection ended.
 */
public enum QuitReason {
    /**
     * The player disconnected.
     */
    DISCONNECTED,
    /**
     * The server kicked or disconnected the player.
     */
    KICKED,
    /**
     * The connection stopped responding.
     */
    TIMED_OUT,
    /**
     * The connection ended because of invalid data or an error while handling it.
     */
    ERRONEOUS_STATE
}
