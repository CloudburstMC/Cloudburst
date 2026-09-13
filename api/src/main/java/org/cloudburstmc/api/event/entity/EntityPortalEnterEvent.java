package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Location;

import java.util.Objects;

/**
 * Fired when an entity comes into contact with a portal.
 * Cancelling this event prevents the portal from processing the entity for that tick.
 */
public class EntityPortalEnterEvent extends EntityEvent implements Cancellable {

    private final Location location;
    private final PortalType type;

    /**
     * Creates a portal-enter event.
     *
     * @param entity   the entity entering the portal
     * @param location the portal block touched by the entity
     * @param type     the portal type
     */
    public EntityPortalEnterEvent(Entity entity, Location location, PortalType type) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.location = Objects.requireNonNull(location, "location");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Returns the portal block touched by the entity.
     *
     * @return the portal location
     */
    public Location getLocation() {
        return this.location;
    }

    /**
     * Returns the kind of portal entered.
     *
     * @return the portal type
     */
    public PortalType getPortalType() {
        return this.type;
    }
}
