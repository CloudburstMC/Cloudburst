package org.cloudburstmc.api.event.vehicle;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;

import static java.util.Objects.requireNonNull;

/**
 * Called before lethal damage destroys a vehicle. Cancellation prevents destruction
 * without cancelling the accepted hit or its hurt animation.
 */
public class VehicleDestroyEvent extends VehicleEvent implements Cancellable {

    private final DamageSource damageSource;

    /**
     * @param vehicle      the vehicle being destroyed
     * @param damageSource the type and origin of lethal damage
     */
    public VehicleDestroyEvent(Vehicle vehicle, DamageSource damageSource) {
        super(vehicle);
        this.damageSource = requireNonNull(damageSource, "damageSource");
    }

    /**
     * @return the type and origin of lethal damage
     */
    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    /**
     * Returns the direct attacker, such as the projectile rather than its shooter.
     *
     * @return the attacking entity, or {@code null} for environmental damage
     */
    public @Nullable Entity getAttacker() {
        return this.damageSource.getDirectEntity();
    }
}
