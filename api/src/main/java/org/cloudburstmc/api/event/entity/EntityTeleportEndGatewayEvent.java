package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.blockentity.EndGateway;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Location;

/**
 * Fired before an entity is teleported by an End gateway.
 */
public class EntityTeleportEndGatewayEvent extends EntityTeleportEvent {

    private final EndGateway gateway;

    /**
     * Creates an End gateway teleport event.
     *
     * @param entity  the entity being teleported
     * @param from    the current location
     * @param to      the requested destination
     * @param gateway the gateway causing the teleport
     */
    public EntityTeleportEndGatewayEvent(Entity entity, Location from, Location to, EndGateway gateway) {
        super(entity, from, to);
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
