package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockLootContext;
import org.cloudburstmc.api.item.ItemStack;

import java.util.List;

/**
 * Resolves the item stacks produced when a block is broken.
 */
@FunctionalInterface
public interface BlockLootHandler {

    /**
     * Resolves the block's item drops for a break operation.
     *
     * @param block the block being broken
     * @param context the loot context
     * @return the item stacks to drop
     */
    List<ItemStack> execute(Block block, BlockLootContext context);
}
