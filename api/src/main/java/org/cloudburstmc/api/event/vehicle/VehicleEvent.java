package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Event;

import static java.util.Objects.requireNonNull;

/**
 * Base event for changes involving a vehicle.
 */
public abstract class VehicleEvent extends Event {

    private final Vehicle vehicle;

    /**
     * @param vehicle the vehicle involved in the event
     */
    protected VehicleEvent(Vehicle vehicle) {
        this.vehicle = requireNonNull(vehicle, "vehicle");
    }

    /**
     * @return the vehicle involved in the event
     */
    public Vehicle getVehicle() {
        return this.vehicle;
    }
}
