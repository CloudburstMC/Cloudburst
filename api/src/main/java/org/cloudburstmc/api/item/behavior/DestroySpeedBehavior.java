package org.cloudburstmc.api.item.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

public interface DestroySpeedBehavior {

    float getDestroySpeed(Behavior<Executor> behavior, ItemStack itemStack, Block block);

    @FunctionalInterface
    interface Executor {

        float execute(ItemStack itemStack, Block block);
    }
}
