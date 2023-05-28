package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

public final class PlayerRespawnEvent extends PlayerEvent {

    private Location location;

    private final boolean firstSpawn;

    public PlayerRespawnEvent(Player player, Location location) {
        this(player, location, false);
    }

    public PlayerRespawnEvent(Player player, Location location, boolean firstSpawn) {
        super(player);
        this.location = location;
        this.firstSpawn = firstSpawn;
    }

    public Location getRespawnLocation() {
        return location;
    }

    public void setRespawnLocation(Location location) {
        this.location = location;
    }

    public boolean isFirstSpawn() {
        return firstSpawn;
    }
}
