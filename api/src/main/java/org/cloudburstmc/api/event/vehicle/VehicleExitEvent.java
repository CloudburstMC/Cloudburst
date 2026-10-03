package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;

import static java.util.Objects.requireNonNull;

/**
 * Called before an entity leaves a vehicle. Forced exits cannot be canceled.
 */
public class VehicleExitEvent extends VehicleEvent implements Cancellable {
    private final Entity exited;
    private final boolean cancellable;

    /**
     * @param vehicle the vehicle being exited
     * @param exited  the entity attempting to leave
     */
    public VehicleExitEvent(Vehicle vehicle, Entity exited) {
        this(vehicle, exited, true);
    }

    /**
     * @param vehicle     the vehicle being exited
     * @param exited      the entity leaving
     * @param cancellable whether cancellation may prevent the exit
     */
    public VehicleExitEvent(Vehicle vehicle, Entity exited, boolean cancellable) {
        super(vehicle);
        this.exited = requireNonNull(exited, "exited");
        this.cancellable = cancellable;
    }

    /**
     * @return the entity attempting to leave
     */
    public Entity getExited() {
        return this.exited;
    }

    /**
     * @return whether cancellation can keep the entity mounted
     */
    public boolean isCancellable() {
        return this.cancellable;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        if (!cancelled || this.cancellable) {
            super.setCancelled(cancelled);
        }
    }
}
