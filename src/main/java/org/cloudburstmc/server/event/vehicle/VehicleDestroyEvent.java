package org.cloudburstmc.server.event.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

public class VehicleDestroyEvent extends VehicleEvent implements Cancellable {

    private final Entity attacker;

    public VehicleDestroyEvent(Entity vehicle, Entity attacker) {
        super(vehicle);
        this.attacker = attacker;
    }

    public Entity getAttacker() {
        return attacker;
    }

}
