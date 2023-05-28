package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Event;

/**
 * Created by larryTheCoder at 7/5/2017.
 * <p>
 * Nukkit Project
 */
public abstract class VehicleEvent extends Event {

    private final Vehicle vehicle;

    public VehicleEvent(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}
