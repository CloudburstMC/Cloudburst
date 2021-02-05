package org.cloudburstmc.api.event.vehicle;


import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

public class VehicleCreateEvent extends VehicleEvent implements Cancellable {

    public VehicleCreateEvent(Entity vehicle) {
        super(vehicle);
    }

}
