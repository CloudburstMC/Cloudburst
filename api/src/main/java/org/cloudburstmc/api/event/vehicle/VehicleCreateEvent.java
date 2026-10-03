package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;

/**
 * Called when a vehicle is spawned. Cancellation removes the new vehicle.
 */
public class VehicleCreateEvent extends VehicleEvent implements Cancellable {

    /**
     * @param vehicle the newly spawned vehicle
     */
    public VehicleCreateEvent(Vehicle vehicle) {
        super(vehicle);
    }
}
