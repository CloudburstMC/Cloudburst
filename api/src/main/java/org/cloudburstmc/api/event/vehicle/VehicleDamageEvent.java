package org.cloudburstmc.api.event.vehicle;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;

import static java.util.Objects.requireNonNull;

/**
 * Called before accepted damage changes a vehicle's health or destroys it.
 * Cancellation prevents the hit from damaging or destroying the vehicle.
 */
public class VehicleDamageEvent extends VehicleEvent implements Cancellable {

    private final DamageSource damageSource;
    private float damage;

    /**
     * @param vehicle      the damaged vehicle
     * @param damageSource the type and origin of damage
     * @param damage       the finite, non-negative damage before reductions
     */
    public VehicleDamageEvent(Vehicle vehicle, DamageSource damageSource, float damage) {
        super(vehicle);
        this.damageSource = requireNonNull(damageSource, "damageSource");
        this.setDamage(damage);
    }

    /**
     * @return the type and origin of damage
     */
    public DamageSource getDamageSource() {
        return this.damageSource;
    }

    /**
     * @return the entity responsible for damage, or {@code null} for environmental damage
     */
    public @Nullable Entity getAttacker() {
        return this.damageSource.getCausingEntity();
    }

    /**
     * @return the damage before reductions
     */
    public float getDamage() {
        return this.damage;
    }

    /**
     * @param damage the finite, non-negative damage before reductions
     * @throws IllegalArgumentException if the damage is negative or not finite
     */
    public void setDamage(float damage) {
        if (!Float.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException("Vehicle damage must be finite and non-negative");
        }

        this.damage = damage;
    }
}
