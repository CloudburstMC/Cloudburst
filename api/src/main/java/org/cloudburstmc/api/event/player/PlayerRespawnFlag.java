package org.cloudburstmc.api.event.player;

/**
 * Additional circumstances of a player respawn.
 */
public enum PlayerRespawnFlag {
    /**
     * The player is respawning at a charged respawn anchor.
     */
    ANCHOR_SPAWN,
    /**
     * The player is respawning at an unobstructed bed.
     */
    BED_SPAWN
}
