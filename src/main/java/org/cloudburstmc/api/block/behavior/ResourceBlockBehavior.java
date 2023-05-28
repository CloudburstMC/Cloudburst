package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

import java.util.random.RandomGenerator;

public interface ResourceBlockBehavior {

    ItemStack getResource(Behavior<Executor> behavior, Block block, RandomGenerator random, int bonusLevel);

    @FunctionalInterface
    interface Executor {
        ItemStack execute(Block block, RandomGenerator random, int bonusLevel);
    }
}
