package org.cloudburstmc.api.event.player;

/**
 * The result of an asynchronous player login check.
 */
public enum PlayerLoginResult {
    /**
     * The connection may continue.
     */
    ALLOWED,
    /**
     * The server has no available player slots.
     */
    KICK_FULL,
    /**
     * The profile is banned.
     */
    KICK_BANNED,
    /**
     * The profile is not permitted by the allow list.
     */
    KICK_WHITELIST,
    /**
     * Login was denied for another reason.
     */
    KICK_OTHER
}
