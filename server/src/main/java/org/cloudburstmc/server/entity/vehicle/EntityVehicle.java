package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Interactable;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.vehicle.VehicleDamageEvent;
import org.cloudburstmc.api.event.vehicle.VehicleDestroyEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

public abstract class EntityVehicle extends CloudEntity implements Vehicle, Interactable {

    public EntityVehicle(EntityType<?> type, Location location) {
        super(type, location);
    }

    public int getRollingAmplitude() {
        return this.data.get(HURT_TICKS);
    }

    public void setRollingAmplitude(int time) {
        this.data.set(HURT_TICKS, time);
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
    protected boolean prepareDamage(CloudEntityDamageEvent source) {
        Entity attacker = source.getDamageSource().getCausingEntity();
        VehicleDamageEvent event = new VehicleDamageEvent(this, source.getDamageSource(), source.getDamage());
        getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            source.setCancelled(true);
            return false;
        }

        source.setDamage(event.getDamage());
        if (source.getDamage() <= 0) {
            return false;
        }

        boolean instantKill = attacker instanceof CloudPlayer player && player.isCreative();
        if (instantKill) {
            source.setDamage(Math.max(source.getDamage(), this.getHealth()));
        }

        if (getHealth() - source.getFinalDamage() <= 0) {
            VehicleDestroyEvent destroyEvent = new VehicleDestroyEvent(this, source.getDamageSource());
            getServer().getEventManager().fire(destroyEvent);

            if (destroyEvent.isCancelled()) {
                source.setCancelled(true);
                return false;
            }
        }

        return true;
    }
}
