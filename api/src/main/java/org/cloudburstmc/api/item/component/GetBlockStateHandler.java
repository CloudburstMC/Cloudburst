package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Optional;

/**
 * Resolves the block state represented by an item for placement.
 */
@FunctionalInterface
public interface GetBlockStateHandler {

    /**
     * Returns the block state, or an empty result if the item does not represent a placeable block.
     */
    Optional<BlockState> execute(ItemStack item);
}
