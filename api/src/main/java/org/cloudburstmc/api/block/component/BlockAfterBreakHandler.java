package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockLootContext;

/**
 * Applies effects after a block is destroyed by a block break.
 */
@FunctionalInterface
public interface BlockAfterBreakHandler {

    /**
     * @param block   the block snapshot from before destruction
     * @param context the original tool and entity responsible for the break
     */
    void execute(Block block, BlockLootContext context);
}
