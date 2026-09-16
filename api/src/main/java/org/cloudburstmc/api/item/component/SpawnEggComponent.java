package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.EntityType;

import static java.util.Objects.requireNonNull;

/**
 * Identifies the entity created by a spawn egg.
 *
 * @param entityType entity type created by the item
 */
public record SpawnEggComponent(EntityType<?> entityType) {

    public SpawnEggComponent {
        requireNonNull(entityType, "entityType");
    }
}
