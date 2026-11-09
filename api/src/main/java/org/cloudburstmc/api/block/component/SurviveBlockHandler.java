package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

/**
 * Determines whether the supplied block states can remain at their level position.
 * The states may describe a proposal that has not been placed.
 */
@FunctionalInterface
public interface SurviveBlockHandler {

    /**
     * Evaluates support without changing blocks, firing events or scheduling updates.
     *
     * @param block the captured or proposed block to evaluate
     * @return whether the block can survive
     */
    boolean execute(Block block);
}
