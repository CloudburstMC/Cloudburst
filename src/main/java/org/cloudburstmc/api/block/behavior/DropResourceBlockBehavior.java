package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface DropResourceBlockBehavior {

    DroppedItem dropResource(Behavior<Executor> behavior, Block block, ItemStack itemStack);

    @FunctionalInterface
    interface Executor {

        DroppedItem execute(Block block, ItemStack itemStack);
    }
}
