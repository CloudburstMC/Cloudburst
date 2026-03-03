package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface CanBePlacedOnHandler {

    boolean execute(ItemStack itemStack, Block block);
}
