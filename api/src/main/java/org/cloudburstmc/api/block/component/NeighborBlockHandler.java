package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

/**
 * Handles a change to a neighboring block.
 */
@FunctionalInterface
public interface NeighborBlockHandler {

    /**
     * @param block the block receiving the update
     * @param neighbor the neighboring block that changed
     */
    void execute(Block block, Block neighbor);
}
