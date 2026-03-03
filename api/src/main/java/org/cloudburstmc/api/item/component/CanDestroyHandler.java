package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Handler that determines whether an item can destroy a given block.
 * Used by {@code ItemComponents.CAN_DESTROY}.
 */
@FunctionalInterface
public interface CanDestroyHandler {
    boolean execute(ItemStack item, Block block);
}
