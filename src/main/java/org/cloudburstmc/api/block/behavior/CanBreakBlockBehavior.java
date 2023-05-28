package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface CanBreakBlockBehavior {

    boolean canBreak(Behavior<Executor> behavior, Block block, ItemStack item);

    @FunctionalInterface
    interface Executor {
        boolean execute(Block block, ItemStack item);
    }
}
