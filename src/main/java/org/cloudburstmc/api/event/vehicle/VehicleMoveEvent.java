package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.level.Location;

public final class VehicleMoveEvent extends VehicleEvent {

    private final Location from;
    private final Location to;

    public VehicleMoveEvent(Vehicle vehicle, Location from, Location to) {
        super(vehicle);
        this.from = from;
        this.to = to;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }
}
