package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Calculates an integer property of an item.
 */
@FunctionalInterface
public interface IntItemHandler {

    /**
     * @param itemStack the item to query
     * @return the property value
     */
    int execute(ItemStack itemStack);
}
