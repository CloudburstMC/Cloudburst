package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface CanBePlacedOnBehavior {

    boolean get(Behavior<CanBePlacedOnBehavior.Executor> behavior, ItemStack item, Block block);

    @FunctionalInterface
    interface Executor {

        boolean execute(ItemStack itemStack, Block block);
    }
}
