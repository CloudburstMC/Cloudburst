package org.cloudburstmc.api.event.vehicle;


import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;

public final class VehicleCreateEvent extends VehicleEvent implements Cancellable {

    public VehicleCreateEvent(Vehicle vehicle) {
        super(vehicle);
    }

}
