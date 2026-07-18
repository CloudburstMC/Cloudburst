package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

import java.util.random.RandomGenerator;

/**
 * Determines the number of resources produced by a block.
 */
@FunctionalInterface
public interface ResourceCountBlockHandler {

    /**
     * @param block the block producing resources
     * @param random the random source
     * @param bonusLevel the applicable bonus level
     * @return the resource count
     */
    int execute(Block block, RandomGenerator random, int bonusLevel);
}
