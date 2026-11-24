package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
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
}
