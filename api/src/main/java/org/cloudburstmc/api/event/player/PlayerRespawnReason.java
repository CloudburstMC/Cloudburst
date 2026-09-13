package org.cloudburstmc.api.event.player;

/**
 * The reason a player is being respawned.
 */
public enum PlayerRespawnReason {
    /**
     * The player is joining the level for the first time.
     */
    INITIAL_SPAWN,
    /**
     * The player died.
     */
    DEATH,
    /**
     * The player returned through the End exit portal.
     */
    END_PORTAL,
    /**
     * A plugin requested the respawn.
     */
    PLUGIN
}
