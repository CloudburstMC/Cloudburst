package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.EntityType;

import static java.util.Objects.requireNonNull;

/**
 * Identifies what type of entity a spawn egg spawns.
 *
 * @param entityType the entity type to spawn
 */
public record SpawnEggComponent(EntityType<?> entityType) {

    public SpawnEggComponent {
        requireNonNull(entityType, "entityType");
    }
}
