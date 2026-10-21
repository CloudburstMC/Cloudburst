package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.LiquidState;

/**
 * Handles a change to the liquid at a block's position.
 */
@FunctionalInterface
public interface LiquidChangeBlockHandler {

    /**
     * Called after the liquid changes.
     *
     * @param block          the block with its updated liquid
     * @param previousLiquid the liquid before the change
     */
    void execute(Block block, LiquidState previousLiquid);
}
