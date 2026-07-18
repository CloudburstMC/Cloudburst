package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Drops an item resource from a block.
 */
@FunctionalInterface
public interface DropResourceBlockHandler {

    /**
     * @param block the source block
     * @param itemStack the item to drop
     * @return the dropped item entity
     */
    DroppedItem execute(Block block, ItemStack itemStack);
}
