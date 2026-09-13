package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Location;

import java.util.Objects;

/**
 * Fired before an entity is teleported.
 */
public class EntityTeleportEvent extends EntityEvent implements Cancellable {

    private final Location from;
    private Location to;

    /**
     * Creates an entity teleport event.
     *
     * @param entity the entity being teleported
     * @param from   the current location
     * @param to     the requested destination
     */
    public EntityTeleportEvent(Entity entity, Location from, Location to) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }

    /**
     * Returns the location from which the entity is teleporting.
     *
     * @return the current location
     */
    public Location getFrom() {
        return this.from;
    }

    /**
     * Returns the teleport destination.
     *
     * @return the destination
     */
    public Location getTo() {
        return this.to;
    }

    /**
     * Changes the teleport destination.
     *
     * @param to the new destination
     */
    public void setTo(Location to) {
        this.to = Objects.requireNonNull(to, "to");
    }
}
