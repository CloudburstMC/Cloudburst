package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.entity.EntityEvent;
import org.cloudburstmc.server.entity.Entity;

public class EntityVehicleEnterEvent extends EntityEvent implements Cancellable {

    private final Entity vehicle;

    public EntityVehicleEnterEvent(Entity entity, Entity vehicle) {
        this.entity = entity;
        this.vehicle = vehicle;
    }

    public Entity getVehicle() {
        return vehicle;
    }

}
