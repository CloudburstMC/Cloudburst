package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Location;

import static java.util.Objects.requireNonNull;

/**
 * Called before an entity is added to a level.
 */
public class EntitySpawnEvent extends EntityEvent implements Cancellable {

    public EntitySpawnEvent(Entity entity) {
        this.entity = requireNonNull(entity, "entity");
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
