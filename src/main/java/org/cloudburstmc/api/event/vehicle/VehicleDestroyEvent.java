package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;

public class VehicleDestroyEvent extends VehicleEvent implements Cancellable {

    private final Entity attacker;

    public VehicleDestroyEvent(Vehicle vehicle, Entity attacker) {
        super(vehicle);
        this.attacker = attacker;
    }

    public Entity getAttacker() {
        return attacker;
    }

}
