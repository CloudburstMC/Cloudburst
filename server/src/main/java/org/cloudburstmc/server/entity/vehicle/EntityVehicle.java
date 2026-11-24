package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.entity.EntitySpawnEvent;
import org.cloudburstmc.api.event.vehicle.VehicleCreateEvent;
import org.cloudburstmc.api.event.vehicle.VehicleDamageEvent;
import org.cloudburstmc.api.event.vehicle.VehicleDestroyEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudEntityData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.function.BooleanSupplier;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;

public abstract class EntityVehicle extends CloudEntity implements Vehicle {

    public EntityVehicle(EntityType<?> type, Location location) {
        this(type, location, new CloudEntityData());
    }

    protected EntityVehicle(EntityType<?> type, Location location, CloudEntityData state) {
        super(type, location, state);
        this.data.set(HURT_TICKS, 0);
        this.data.set(HURT_DIRECTION, 1);
    }

    @Override
    public boolean blocksBuilding() {
        return true;
    }

    @Override
    protected boolean spawn(EntitySpawnEvent event, BooleanSupplier prepareSpawn) {
        return super.spawn(event, () -> {
            VehicleCreateEvent created = new VehicleCreateEvent(this);
            this.server.getEventManager().fire(created);
            if (created.isCancelled()) {
                this.remove(EntityRemoveCause.DISCARD);
                return false;
            }
            return !this.isClosed() && prepareSpawn.getAsBoolean();
        });
    }

    public int getHurtTime() {
        return this.data.require(HURT_TICKS);
    }

    public void setHurtTime(int time) {
        this.data.set(HURT_TICKS, time);
    }

    public int getHurtDirection() {
        return this.data.require(HURT_DIRECTION);
    }

    public void setHurtDirection(int direction) {
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
        if (!this.isClosed() && this.getHurtTime() > 0) {
            this.setHurtTime(Math.max(0, this.getHurtTime() - tickDiff));
            updated = true;
        }

        return updated;
    }

    public String getInteractionPrompt() {
        return this.getPassengers().isEmpty() ? "Mount" : "";
    }

    @Override
    protected boolean prepareDamage(CloudEntityDamageEvent event) {
        DamageSource source = event.getDamageSource();
        Entity attacker = source.getCausingEntity();
        VehicleDamageEvent vehicleDamage = new VehicleDamageEvent(this, source, event.getDamage());
        this.getServer().getEventManager().fire(vehicleDamage);
        if (vehicleDamage.isCancelled()) {
            event.setCancelled(true);
            return false;
        }

        event.setDamage(vehicleDamage.getDamage());
        if (event.getDamage() <= 0) {
            return false;
        }

        boolean instantKill = attacker instanceof CloudPlayer player && player.isCreative();
        if (instantKill) {
            event.setDamage(Math.max(event.getDamage(), this.getHealth()));
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
        setHurtTime(10);
        setHurtDirection(-getHurtDirection());
    }
}
