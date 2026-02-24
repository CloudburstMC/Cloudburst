package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.entity.Entity;

/**
 * A {@link SlotGroup} associated with an entity (e.g. a player or mob).
 * Provides access to the entity that owns this slot group.
 */
public interface EntitySlotGroup extends SlotGroup {

    /**
     * Returns the entity that owns this slot group.
     *
     * @return the holder entity
     */
    Entity getHolder();
}
