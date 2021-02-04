package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.entity.EntityEvent;
import org.cloudburstmc.server.entity.EntityCreature;
import org.cloudburstmc.server.entity.Human;
import org.cloudburstmc.server.entity.projectile.EntityProjectile;
import org.cloudburstmc.server.entity.vehicle.EntityVehicle;
import org.cloudburstmc.server.level.Location;

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
        return this.entity instanceof EntityCreature;
    }

    public boolean isHuman() {
        return this.entity instanceof Human;
    }

    public boolean isProjectile() {
        return this.entity instanceof EntityProjectile;
    }

    public boolean isVehicle() {
        return this.entity instanceof EntityVehicle;
    }

    public boolean isItem() {
        return this.entity instanceof DroppedItem;
    }

}
