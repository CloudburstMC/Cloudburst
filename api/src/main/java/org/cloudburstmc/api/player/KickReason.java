package org.cloudburstmc.api.player;

/**
 * The category of a player kick, independent of the disconnect text shown to the player.
 */
public enum KickReason {
    /**
     * Another connection replaced the player's session.
     */
    NEW_CONNECTION,
    /**
     * An operator or administrative command kicked the player.
     */
    KICKED_BY_ADMIN,
    /**
     * The player is not allowed by the whitelist.
     */
    NOT_WHITELISTED,
    /**
     * The player's IP address is banned.
     */
    IP_BANNED,
    /**
     * The player's identity is banned.
     */
    PLAYER_BANNED,
    /**
     * An entity interaction failed validation.
     */
    INVALID_PVE,
    /**
     * The player did not complete login in time.
     */
    LOGIN_TIMEOUT,
    /**
     * The server's player limit has been reached.
     */
    SERVER_FULL,
    /**
     * The player was detected flying when flight was not allowed.
     */
    FLYING_DISABLED,
    /**
     * No more specific category was supplied.
     */
    UNKNOWN
}
