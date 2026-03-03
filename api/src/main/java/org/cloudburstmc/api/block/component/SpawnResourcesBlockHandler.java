package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

import java.util.random.RandomGenerator;

@FunctionalInterface
public interface SpawnResourcesBlockHandler {

    void execute(Block block, RandomGenerator random, ItemStack tool, int bonusLootLevel);
}
