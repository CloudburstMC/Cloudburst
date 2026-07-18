package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;

import java.util.random.RandomGenerator;

/**
 * Determines the experience dropped by a block state.
 */
@FunctionalInterface
public interface ExpBlockHandler {

    /**
     * @param state the block state being destroyed
     * @param random the random source
     * @return the experience to drop
     */
    int execute(BlockState state, RandomGenerator random);
}
