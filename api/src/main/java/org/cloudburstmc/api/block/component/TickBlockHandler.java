package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

import java.util.random.RandomGenerator;

/**
 * Handles a block tick.
 */
@FunctionalInterface
public interface TickBlockHandler {

    /**
     * @param block the block being ticked
     * @param random the random source
     */
    void execute(Block block, RandomGenerator random);
}
