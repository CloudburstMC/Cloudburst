package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

import java.util.random.RandomGenerator;

/**
 * Spawns the resources produced when a block is destroyed.
 */
@FunctionalInterface
public interface SpawnResourcesBlockHandler {

    /**
     * @param block the destroyed block
     * @param random the random source
     * @param tool the item used to destroy the block
     * @param bonusLootLevel the applicable bonus loot level
     */
    void execute(Block block, RandomGenerator random, ItemStack tool, int bonusLootLevel);
}
