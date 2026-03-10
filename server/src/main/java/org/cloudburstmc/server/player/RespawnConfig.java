package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;

/**
 * Stores a player's personal respawn point, set by sleeping in a bed or
 * interacting with a charged respawn anchor.
 *
 * <p>A {@code RespawnConfig} is considered <em>forced</em> when the spawn point was set
 * by a command or plugin rather than through normal bed/anchor interaction. A forced spawn
 * is used as-is even if the block no longer exists.</p>
 */
public record RespawnConfig(
        @NonNull CloudLevel level,
        @NonNull Vector3i pos,
        float yaw,
        boolean forced,
        @NonNull SpawnType spawnType
) {
    /**
     * The spawn type; used to pick the right validation path on respawn.
     */
    public enum SpawnType {
        /**
         * Spawn was set by sleeping in a bed.
         */
        BED,
        /**
         * Spawn was set by interacting with a charged respawn anchor.
         */
        RESPAWN_ANCHOR
    }
}
