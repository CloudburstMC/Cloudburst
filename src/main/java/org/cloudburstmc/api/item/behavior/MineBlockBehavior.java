package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface MineBlockBehavior {

    ItemStack mineBlock(Behavior<Executor> behavior, ItemStack itemStack, Block block, Entity owner);

    @FunctionalInterface
    interface Executor {

        ItemStack mineBlock(ItemStack itemStack, Block block, Entity owner);
    }
}
