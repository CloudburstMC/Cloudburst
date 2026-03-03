package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface DestroySpeedHandler {

    float execute(ItemStack itemStack, Block block);
}
