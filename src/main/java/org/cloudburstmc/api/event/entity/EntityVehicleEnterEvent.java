package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;

public class EntityVehicleEnterEvent extends EntityEvent implements Cancellable {

    private final Vehicle vehicle;

    public EntityVehicleEnterEvent(Entity entity, Vehicle vehicle) {
        this.entity = entity;
        this.vehicle = vehicle;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

}
