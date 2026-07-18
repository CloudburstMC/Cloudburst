package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Applies an item's general use behavior.
 */
@FunctionalInterface
public interface UseHandler {

    /**
     * @param itemStack the item being used
     * @param entity the entity using the item
     * @return the resulting item
     */
    ItemStack execute(ItemStack itemStack, Entity entity);
}
