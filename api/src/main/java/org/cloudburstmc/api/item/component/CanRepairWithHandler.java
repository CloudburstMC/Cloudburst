package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Tests whether a material item can repair another item.
 */
@FunctionalInterface
public interface CanRepairWithHandler {

    /**
     * Returns whether the material can repair the item.
     *
     * @param item     item being repaired
     * @param material candidate repair material
     * @return {@code true} if the material can repair the item
     */
    boolean execute(ItemStack item, ItemStack material);
}
