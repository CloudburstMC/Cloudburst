package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface CanBreakBlockHandler {

    boolean execute(Block block, ItemStack item);
}
