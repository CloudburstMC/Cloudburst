package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;

import static java.util.Objects.requireNonNull;

/**
 * Called before an entity enters a vehicle. Cancellation prevents mounting.
 */
public class VehicleEnterEvent extends VehicleEvent implements Cancellable {
    private final Entity entered;

    /**
     * @param vehicle the vehicle being entered
     * @param entered the entity attempting to enter
     */
    public VehicleEnterEvent(Vehicle vehicle, Entity entered) {
        super(vehicle);
        this.entered = requireNonNull(entered, "entered");
    }

    /**
     * @return the entity attempting to enter
     */
    public Entity getEntered() {
        return this.entered;
    }
}
