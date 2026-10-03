package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Interactable;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.entity.EntitySpawnEvent;
import org.cloudburstmc.api.event.vehicle.VehicleCreateEvent;
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
        this.data.set(HURT_TICKS, 0);
        this.data.set(HURT_DIRECTION, 1);
    }

    @Override
    public boolean spawn(EntitySpawnEvent event) {
        if (!super.spawn(event)) {
            return false;
        }

        VehicleCreateEvent created = new VehicleCreateEvent(this);
        this.server.getEventManager().fire(created);
        if (created.isCancelled()) {
            this.close();
            return false;
        }

        return !this.closed;
    }

    public int getRollingAmplitude() {
        return this.data.require(HURT_TICKS);
    }

    public void setRollingAmplitude(int time) {
        this.data.set(HURT_TICKS, time);
    }

    public int getRollingDirection() {
        return this.data.require(HURT_DIRECTION);
    }

    public void setRollingDirection(int direction) {
        this.data.set(HURT_DIRECTION, direction);
    }

    public int getDamage() {
        return this.data.require(STRUCTURAL_INTEGRITY);
    }

    public void setDamage(int damage) {
        this.data.set(STRUCTURAL_INTEGRITY, damage);
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean updated = super.entityBaseTick(tickDiff);
        if (!this.closed && this.getRollingAmplitude() > 0) {
            this.setRollingAmplitude(Math.max(0, this.getRollingAmplitude() - tickDiff));
            updated = true;
        }

        return updated;
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

        return true;
    }

    @Override
    protected boolean tryPreventDeath(DamageSource source) {
        VehicleDestroyEvent event = new VehicleDestroyEvent(this, source);
        this.server.getEventManager().fire(event);
        return event.isCancelled();
    }

    protected void performHurtAnimation() {
        setRollingAmplitude(10);
        setRollingDirection(-getRollingDirection());
    }
}
