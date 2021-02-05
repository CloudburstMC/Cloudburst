package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.Entity;

public class VehicleUpdateEvent extends VehicleEvent {

    public VehicleUpdateEvent(Entity vehicle) {
        super(vehicle);
    }

}
