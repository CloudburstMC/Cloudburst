package org.cloudburstmc.api.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.behavior.Behavior;

import java.util.random.RandomGenerator;

public interface SpawnResourcesBlockBehavior {

    void spawnResource(Behavior<Executor> behavior, Block block, RandomGenerator random, ItemStack tool, int bonusLootLevel);

    @FunctionalInterface
    interface Executor {

        void execute(Block block, RandomGenerator random, ItemStack tool, int bonusLootLevel);
    }
}
