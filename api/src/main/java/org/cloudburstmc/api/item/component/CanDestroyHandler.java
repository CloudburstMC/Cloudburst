package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Determines whether an item can destroy a block.
 */
@FunctionalInterface
public interface CanDestroyHandler {

    /**
     * @param item the item being used
     * @param block the target block
     * @return whether the item can destroy the block
     */
    boolean execute(ItemStack item, Block block);
}
