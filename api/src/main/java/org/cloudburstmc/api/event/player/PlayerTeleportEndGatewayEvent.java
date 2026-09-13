package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.blockentity.EndGateway;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

/**
 * Fired before a player is teleported by an End gateway.
 */
public class PlayerTeleportEndGatewayEvent extends PlayerTeleportEvent {

    private final EndGateway gateway;

    /**
     * Creates an End gateway teleport event.
     *
     * @param player  the player being teleported
     * @param from    the current location
     * @param to      the requested destination
     * @param gateway the gateway causing the teleport
     */
    public PlayerTeleportEndGatewayEvent(Player player, Location from, Location to, EndGateway gateway) {
        super(player, from, to, PlayerTeleportCause.END_GATEWAY);
        this.gateway = gateway;
    }

    /**
     * Returns the gateway causing the teleport.
     *
     * @return the End gateway
     */
    public EndGateway getGateway() {
        return this.gateway;
    }
}
