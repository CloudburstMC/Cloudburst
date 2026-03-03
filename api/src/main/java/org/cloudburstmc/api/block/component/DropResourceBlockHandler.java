package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface DropResourceBlockHandler {

    DroppedItem execute(Block block, ItemStack itemStack);
}
