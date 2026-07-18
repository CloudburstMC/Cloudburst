package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;

import java.util.random.RandomGenerator;

/**
 * Determines the item resource produced by a block.
 */
@FunctionalInterface
public interface ResourceBlockHandler {

    /**
     * @param block the block producing the resource
     * @param random the random source
     * @param bonusLevel the applicable bonus level
     * @return the produced item
     */
    ItemStack execute(Block block, RandomGenerator random, int bonusLevel);
}
