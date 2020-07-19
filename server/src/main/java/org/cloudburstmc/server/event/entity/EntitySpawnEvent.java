package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.EntityCreature;
import org.cloudburstmc.server.entity.impl.Human;
import org.cloudburstmc.server.entity.impl.projectile.EntityProjectile;
import org.cloudburstmc.server.entity.impl.vehicle.EntityVehicle;
import org.cloudburstmc.server.entity.misc.DroppedItem;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.level.Location;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntitySpawnEvent extends EntityEvent {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public EntitySpawnEvent(Entity entity) {
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
