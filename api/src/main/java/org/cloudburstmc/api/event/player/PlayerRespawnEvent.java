package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Fired just before a player is moved to a respawn position after joining, dying, or
 * returning through an End portal.
 */
public class PlayerRespawnEvent extends PlayerEvent {

    private final PlayerRespawnReason reason;
    private final Set<PlayerRespawnFlag> flags;
    private Location location;

    /**
     * Creates a player respawn event.
     *
     * @param player   the player being respawned
     * @param location the destination of the respawn
     * @param reason   the reason for the respawn
     * @param flags    additional circumstances of the respawn
     */
    public PlayerRespawnEvent(Player player, Location location, PlayerRespawnReason reason, Set<PlayerRespawnFlag> flags) {
        super(player);
        this.location = Objects.requireNonNull(location, "location");
        this.reason = Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(flags, "flags");
        this.flags = Collections.unmodifiableSet(flags.isEmpty() ? EnumSet.noneOf(PlayerRespawnFlag.class) : EnumSet.copyOf(flags));
    }

    /**
     * Returns the location the player will be teleported to on respawn.
     *
     * @return the respawn location
     */
    public Location getRespawnLocation() {
        return location;
    }

    /**
     * Changes the location the player will be teleported to on respawn.
     *
     * @param location the respawn location
     */
    public void setRespawnLocation(Location location) {
        this.location = Objects.requireNonNull(location, "location");
    }

    /**
     * Returns why the player is being respawned.
     *
     * @return the respawn reason
     */
    public PlayerRespawnReason getReason() {
        return this.reason;
    }

    /**
     * Returns the circumstances of this respawn.
     *
     * @return an immutable set of respawn flags
     */
    public Set<PlayerRespawnFlag> getRespawnFlags() {
        return flags;
    }

    /**
     * Returns whether this respawn has the given flag.
     *
     * @param flag the flag to test
     * @return whether the flag is present
     */
    public boolean hasFlag(PlayerRespawnFlag flag) {
        return flags.contains(flag);
    }
}
