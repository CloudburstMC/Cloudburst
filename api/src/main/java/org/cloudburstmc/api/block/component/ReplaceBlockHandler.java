package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockPlacementContext;
import org.cloudburstmc.api.block.BlockState;

/**
 * Determines whether block placement may replace an existing block.
 */
@FunctionalInterface
public interface ReplaceBlockHandler {

    /**
     * Checks whether a placed block may replace the current block.
     *
     * @param block       the candidate block, which may differ from the clicked block
     * @param replacement the state being placed
     * @param context     the original interaction
     * @return whether replacement is allowed
     */
    boolean execute(Block block, BlockState replacement, BlockPlacementContext context);
}
