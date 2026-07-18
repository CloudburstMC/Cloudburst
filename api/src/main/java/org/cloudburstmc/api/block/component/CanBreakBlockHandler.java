package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Determines whether an item can break a block.
 */
@FunctionalInterface
public interface CanBreakBlockHandler {

    /**
     * @param block the block being broken
     * @param item the item used to break the block
     * @return whether the item can break the block
     */
    boolean execute(Block block, ItemStack item);
}
