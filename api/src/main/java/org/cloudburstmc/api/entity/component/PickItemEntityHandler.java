package org.cloudburstmc.api.entity.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Resolves the item representing an entity when it is picked.
 */
@FunctionalInterface
public interface PickItemEntityHandler {

    /**
     * Returns an item without changing the entity or a player's inventory.
     *
     * @param entity      the entity being picked
     * @param includeData whether supported entity-specific data should be included
     * @return the picked item, or {@link ItemStack#EMPTY} when the entity has no item representation
     */
    ItemStack execute(Entity entity, boolean includeData);
}
