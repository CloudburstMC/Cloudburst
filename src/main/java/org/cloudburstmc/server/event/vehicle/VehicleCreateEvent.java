package org.cloudburstmc.server.event.vehicle;


import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.entity.Entity;

public class VehicleCreateEvent extends VehicleEvent implements Cancellable {

    public VehicleCreateEvent(Entity vehicle) {
        super(vehicle);
    }

}
