package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

public class EntityEnterVehicleEvent extends VehicleEvent implements Cancellable {

    private final Entity rider;

    public EntityEnterVehicleEvent(Entity rider, Vehicle vehicle) {
        super(vehicle);
        this.rider = rider;
    }

    public Entity getEntity() {
        return rider;
    }

    public boolean isPlayer() {
        return rider instanceof Player;
    }

}
