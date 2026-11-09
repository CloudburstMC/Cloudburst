package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Determines what a falling block drops if it breaks on landing.
 */
@FunctionalInterface
public interface FallingBlockDropHandler {

    /**
     * @param state the falling block state
     * @return the item to drop, or {@link ItemStack#EMPTY} for no drop
     */
    ItemStack execute(BlockState state);
}
