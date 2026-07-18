package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Determines the item produced by picking a block.
 */
@FunctionalInterface
public interface PickBlockHandler {

    /**
     * @param block the block being picked
     * @return the picked item
     */
    ItemStack execute(Block block);
}
