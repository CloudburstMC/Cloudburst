package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Evaluates a boolean property of an item.
 */
@FunctionalInterface
public interface BooleanItemHandler {

    /**
     * @param itemStack the item to evaluate
     * @return the property value
     */
    boolean execute(ItemStack itemStack);
}
