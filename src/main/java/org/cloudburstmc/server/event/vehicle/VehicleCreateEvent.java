package org.cloudburstmc.server.event.vehicle;


import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;

public class VehicleCreateEvent extends VehicleEvent implements Cancellable {

    public VehicleCreateEvent(Entity vehicle) {
        super(vehicle);
    }

}
