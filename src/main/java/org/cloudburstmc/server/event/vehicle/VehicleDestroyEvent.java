package org.cloudburstmc.server.event.vehicle;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.entity.Entity;

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
