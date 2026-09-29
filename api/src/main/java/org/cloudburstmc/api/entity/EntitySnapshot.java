package org.cloudburstmc.api.entity;

import org.cloudburstmc.api.level.Location;

/**
 * An immutable copy of an entity's persistent state, independent of the original entity.
 * Copies do not inherit its identity, position, motion, owner, or passengers.
 * Obtain snapshots through {@link Entity#createSnapshot()}.
 */
public interface EntitySnapshot {

    /**
     * Returns the entity type represented by this snapshot.
     *
     * @return the captured type
     */
    EntityType<?> getType();

    /**
     * Creates an initialized copy at the requested location without spawning it.
     * Call {@link Entity#spawn()} to add the copy to the level.
     *
     * @param location the new entity's location and rotation
     * @return a new entity with the captured persistent state and a fresh identity
     * @throws org.cloudburstmc.api.registry.RegistryException if the type has no registered factory
     */
    Entity createEntity(Location location);
}
