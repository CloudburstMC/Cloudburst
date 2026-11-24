package org.cloudburstmc.api.entity;

import org.cloudburstmc.api.level.Location;

/**
 * Constructs entities of a registered type.
 *
 * @param <T> the entity type
 */
@FunctionalInterface
public interface EntityFactory<T extends Entity> {

    /**
     * Constructs an entity at the given location without spawning it.
     */
    T create(EntityType<T> type, Location location);
}
