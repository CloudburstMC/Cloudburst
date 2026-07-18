package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

/**
 * Determines whether a block can remain at its current position.
 */
@FunctionalInterface
public interface SurviveBlockHandler {

    /**
     * @param block the block to evaluate
     * @return whether the block can survive
     */
    boolean execute(Block block);
}
