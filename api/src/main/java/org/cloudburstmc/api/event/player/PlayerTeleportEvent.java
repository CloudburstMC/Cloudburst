package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Fired before a player is teleported.
 */
public class PlayerTeleportEvent extends PlayerEvent implements Cancellable {

    private final PlayerTeleportCause cause;
    private final Location from;
    private Location to;

    /**
     * Creates a player teleport event.
     *
     * @param player the player being teleported
     * @param from   the current location
     * @param to     the requested destination
     * @param cause  the cause of the teleport
     */
    public PlayerTeleportEvent(Player player, Location from, Location to, PlayerTeleportCause cause) {
        super(player);
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    /**
     * Returns the location from which the player is teleporting.
     *
     * @return the current location
     */
    public Location getFrom() {
        return from;
    }

    /**
     * Returns the teleport destination.
     *
     * @return the destination
     */
    public Location getTo() {
        return to;
    }

    /**
     * Changes the destination of this teleport.
     *
     * @param to the new destination
     */
    public void setTo(Location to) {
        this.to = Objects.requireNonNull(to, "to");
    }

    /**
     * Returns the cause of this teleport.
     *
     * @return the teleport cause
     */
    public PlayerTeleportCause getCause() {
        return cause;
    }
}
