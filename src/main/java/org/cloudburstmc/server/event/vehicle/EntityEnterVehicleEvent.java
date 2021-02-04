package org.cloudburstmc.server.event.vehicle;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.player.Player;

public class EntityEnterVehicleEvent extends VehicleEvent implements Cancellable {

    private final Entity riding;

    public EntityEnterVehicleEvent(Entity riding, Entity vehicle) {
        super(vehicle);
        this.riding = riding;
    }

    public Entity getEntity() {
        return riding;
    }

    public boolean isPlayer() {
        return riding instanceof Player;
    }

}
