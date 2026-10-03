package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.level.Location;

/**
 * Called after a vehicle changes position or rotation.
 */
public class VehicleMoveEvent extends VehicleEvent {

    private final Location from;
    private final Location to;

    /**
     * @param vehicle the moving vehicle
     * @param from    the location before movement
     * @param to      the location after movement
     */
    public VehicleMoveEvent(Vehicle vehicle, Location from, Location to) {
        super(vehicle);
        this.from = java.util.Objects.requireNonNull(from, "from");
        this.to = java.util.Objects.requireNonNull(to, "to");
    }

    /**
     * @return the location before movement
     */
    public Location getFrom() {
        return from;
    }

    /**
     * @return the location after movement
     */
    public Location getTo() {
        return to;
    }
}
