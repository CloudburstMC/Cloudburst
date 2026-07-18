package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Calculates a floating-point property of an item.
 */
@FunctionalInterface
public interface FloatItemHandler {

    /**
     * @param itemStack the item to query
     * @return the property value
     */
    float execute(ItemStack itemStack);
}
