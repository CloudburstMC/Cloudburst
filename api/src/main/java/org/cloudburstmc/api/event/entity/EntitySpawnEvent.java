package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Location;

import static java.util.Objects.requireNonNull;

/**
 * Fired before an entity is added to a level.
 */
public class EntitySpawnEvent extends EntityEvent implements Cancellable {

    /**
     * Creates an entity spawn event.
     *
     * @param entity the entity being spawned
     */
    public EntitySpawnEvent(Entity entity) {
        this.entity = requireNonNull(entity, "entity");
    }

    /**
     * Returns the location at which the entity will spawn.
     *
     * @return the spawn location
     */
    public Location getLocation() {
        return this.entity.getLocation();
    }

    /**
     * Returns the entity type being spawned.
     *
     * @return the entity type
     */
    public EntityType<?> getType() {
        return this.entity.getType();
    }

    /**
     * @return whether the entity is a creature
     */
    public boolean isCreature() {
        return this.entity instanceof Creature;
    }

    /**
     * @return whether the entity is human
     */
    public boolean isHuman() {
        return this.entity instanceof Human;
    }

    /**
     * @return whether the entity is a projectile
     */
    public boolean isProjectile() {
        return this.entity instanceof Projectile;
    }

    /**
     * @return whether the entity is a vehicle
     */
    public boolean isVehicle() {
        return this.entity instanceof Vehicle;
    }

    /**
     * @return whether the entity is a dropped item
     */
    public boolean isItem() {
        return this.entity instanceof DroppedItem;
    }
}
