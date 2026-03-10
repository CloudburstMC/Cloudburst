package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

/**
 * Fired after a player has fully respawned and been teleported to their respawn position.
 *
 * <p>This event fires <em>after</em> the respawn is complete. Health, food, effects, and
 * position have all been reset/applied. Unlike {@link PlayerRespawnEvent}, this event is
 * <strong>not cancellable</strong> and cannot redirect the respawn location.</p>
 */
public final class PlayerPostRespawnEvent extends PlayerEvent {

    private final Location respawnLocation;
    private final boolean isBedSpawn;
    private final boolean isAnchorSpawn;

    public PlayerPostRespawnEvent(Player player, Location respawnLocation, boolean isBedSpawn, boolean isAnchorSpawn) {
        super(player);
        this.respawnLocation = respawnLocation;
        this.isBedSpawn = isBedSpawn;
        this.isAnchorSpawn = isAnchorSpawn;
    }

    /**
     * The location the player was teleported to during this respawn.
     */
    public Location getRespawnLocation() {
        return respawnLocation;
    }

    /**
     * Whether the player respawned at a bed.
     */
    public boolean isBedSpawn() {
        return isBedSpawn;
    }

    /**
     * Whether the player respawned at a charged respawn anchor.
     */
    public boolean isAnchorSpawn() {
        return isAnchorSpawn;
    }
}
