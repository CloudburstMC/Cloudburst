package org.cloudburstmc.server.event.vehicle;

import org.cloudburstmc.server.entity.Entity;

public class VehicleUpdateEvent extends VehicleEvent {

    public VehicleUpdateEvent(Entity vehicle) {
        super(vehicle);
    }

}
