package org.cloudburstmc.server.event.vehicle;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.entity.Entity;

public class VehicleDamageEvent extends VehicleEvent implements Cancellable {

    private final Entity attacker;
    private double damage;

    public VehicleDamageEvent(Entity vehicle, Entity attacker, double damage) {
        super(vehicle);
        this.attacker = attacker;
        this.damage = damage;
    }

    public Entity getAttacker() {
        return attacker;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

}
