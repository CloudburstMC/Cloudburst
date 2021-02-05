package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.vehicle.Vehicle;

public class VehicleUpdateEvent extends VehicleEvent {

    public VehicleUpdateEvent(Vehicle vehicle) {
        super(vehicle);
    }

}
