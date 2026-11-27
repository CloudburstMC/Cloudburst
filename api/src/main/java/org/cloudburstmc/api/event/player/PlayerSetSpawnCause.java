package org.cloudburstmc.api.event.player;

/**
 * Why a player's spawn point changed.
 */
public enum PlayerSetSpawnCause {
    /**
     * The player slept in a bed.
     */
    BED,
    /**
     * The player used a charged respawn anchor.
     */
    RESPAWN_ANCHOR,
    /**
     * The spawn point was updated while the player respawned.
     */
    PLAYER_RESPAWN,
    /**
     * A command, such as {@code /spawnpoint}, set the spawn point.
     */
    COMMAND,
    /**
     * A plugin set the spawn point.
     */
    PLUGIN,
    /**
     * The spawn point is cleared, so the player respawns at the world spawn.
     */
    RESET,
    /**
     * No more specific cause applies.
     */
    UNKNOWN
}
