package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.vehicle.Vehicle;

/**
 * Called after a vehicle update. This event may occur more than once per tick.
 */
public class VehicleUpdateEvent extends VehicleEvent {

    /**
     * @param vehicle the updated vehicle
     */
    public VehicleUpdateEvent(Vehicle vehicle) {
        super(vehicle);
    }
}
