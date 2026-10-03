package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;

import static java.util.Objects.requireNonNull;

/**
 * Called before a vehicle pushes another entity. Cancellation prevents that push.
 */
public class VehicleEntityCollisionEvent extends VehicleEvent implements Cancellable {

    private final Entity entity;

    /**
     * @param vehicle the colliding vehicle
     * @param entity  the other entity
     */
    public VehicleEntityCollisionEvent(Vehicle vehicle, Entity entity) {
        super(vehicle);
        this.entity = requireNonNull(entity, "entity");
    }

    /**
     * @return the entity the vehicle collided with
     */
    public Entity getEntity() {
        return this.entity;
    }
}
