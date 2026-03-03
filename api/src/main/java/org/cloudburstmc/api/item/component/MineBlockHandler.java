package org.cloudburstmc.api.item.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;

@FunctionalInterface
public interface MineBlockHandler {

    ItemStack execute(ItemStack itemStack, Block block, Entity owner);
}
