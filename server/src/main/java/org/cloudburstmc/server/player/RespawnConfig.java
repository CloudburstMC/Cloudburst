package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;

/**
 * Stores a player's personal respawn point and how to resolve it.
 */
public record RespawnConfig(
        @NonNull CloudLevel level,
        @NonNull Vector3i pos,
        float yaw,
        @NonNull SpawnType spawnType
) {
    /**
     * Determines whether respawning depends on a specific block.
     */
    public enum SpawnType {
        /**
         * Spawn was set by sleeping in a bed.
         */
        BED,
        /**
         * Spawn was set by interacting with a charged respawn anchor.
         */
        RESPAWN_ANCHOR,
        /**
         * Spawn was set directly by a command or plugin and does not require a bed or anchor.
         */
        FORCED
    }
}
