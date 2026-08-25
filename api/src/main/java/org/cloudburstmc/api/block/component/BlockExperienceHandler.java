package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockLootContext;

/**
 * Resolves the experience produced when a block is broken.
 */
@FunctionalInterface
public interface BlockExperienceHandler {

    /**
     * Resolves the block's experience for a break operation.
     *
     * @param block the block being broken
     * @param context the loot context
     * @return the experience to drop
     */
    int execute(Block block, BlockLootContext context);
}
