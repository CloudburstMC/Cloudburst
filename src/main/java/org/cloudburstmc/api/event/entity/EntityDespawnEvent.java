package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.level.Location;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityDespawnEvent extends EntityEvent {

    public EntityDespawnEvent(Entity entity) {
        this.entity = entity;
    }

    public Location getLocation() {
        return this.entity.getLocation();
    }

    public EntityType<?> getType() {
        return this.entity.getType();
    }

    public boolean isCreature() {
        return this.entity instanceof Creature;
    }

    public boolean isHuman() {
        return this.entity instanceof Human;
    }

    public boolean isProjectile() {
        return this.entity instanceof Projectile;
    }

    public boolean isVehicle() {
        return this.entity instanceof Vehicle;
    }

    public boolean isItem() {
        return this.entity instanceof DroppedItem;
    }

}
