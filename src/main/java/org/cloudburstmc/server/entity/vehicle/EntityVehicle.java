package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Interactable;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.vehicle.VehicleDamageEvent;
import org.cloudburstmc.api.event.vehicle.VehicleDestroyEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.BaseEntity;
import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.HURT_DIRECTION;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.STRUCTURAL_INTEGRITY;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityVehicle extends BaseEntity implements Vehicle, Interactable {

    public EntityVehicle(EntityType<?> type, Location location) {
        super(type, location);
    }

    public int getRollingAmplitude() {
        return this.data.get(HURT_TIME);
    }

    public void setRollingAmplitude(int time) {
        this.data.set(HURT_TIME, time);
    }

    public int getRollingDirection() {
        return this.data.get(HURT_DIRECTION);
    }

    public void setRollingDirection(int direction) {
        this.data.set(HURT_DIRECTION, direction);
    }

    public int getDamage() {
        return this.data.get(STRUCTURAL_INTEGRITY); // false data name (should be DATA_DAMAGE_TAKEN)
    }

    public void setDamage(int damage) {
        this.data.set(STRUCTURAL_INTEGRITY, damage);
    }

    @Override
    public String getInteractButtonText() {
        return "Mount";
    }

    @Override
    public boolean canDoInteraction() {
        return passengers.isEmpty();
    }

    @Override
    public boolean onUpdate(int currentTick) {
        // The rolling amplitude
        if (getRollingAmplitude() > 0) {
            setRollingAmplitude(getRollingAmplitude() - 1);
        }

        // A killer task
        if (this.getY() < -16) {
            kill();
        }
        // Movement code
        updateMovement();
        return true;
    }

    protected boolean rollingDirection = true;

    protected boolean performHurtAnimation() {
        setRollingAmplitude(9);
        setRollingDirection(rollingDirection ? 1 : -1);
        rollingDirection = !rollingDirection;
        return true;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        VehicleDamageEvent event = new VehicleDamageEvent(this, source.getEntity(), source.getFinalDamage());
        getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        boolean instantKill = false;

        if (source instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
            instantKill = damager instanceof CloudPlayer && ((CloudPlayer) damager).isCreative();
        }

        if (instantKill || getHealth() - source.getFinalDamage() < 1) {
            VehicleDestroyEvent event2 = new VehicleDestroyEvent(this, source.getEntity());
            getServer().getEventManager().fire(event2);

            if (event2.isCancelled()) {
                return false;
            }
        }

        if (instantKill) {
            source.setDamage(1000);
        }

        return super.attack(source);
    }
}
