package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Determines whether an item may be placed on a block.
 */
@FunctionalInterface
public interface CanBePlacedOnHandler {

    /**
     * @param itemStack the item being placed
     * @param block the target block
     * @return whether placement is allowed
     */
    boolean execute(ItemStack itemStack, Block block);
}
