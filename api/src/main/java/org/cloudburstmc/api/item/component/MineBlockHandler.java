package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Applies an item's block-mining behavior.
 */
@FunctionalInterface
public interface MineBlockHandler {

    /**
     * @param itemStack the item being used
     * @param block the block being mined
     * @param owner the entity using the item
     * @return the resulting item
     */
    ItemStack execute(ItemStack itemStack, Block block, Entity owner);
}
