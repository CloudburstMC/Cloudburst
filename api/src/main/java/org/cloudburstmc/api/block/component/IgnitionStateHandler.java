package org.cloudburstmc.api.block.component;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;

/**
 * Resolves the state produced by lighting an existing block, without changing the level.
 */
@FunctionalInterface
public interface IgnitionStateHandler {

    /**
     * @param block the block to light
     * @return the lit state, or {@code null} if the block cannot be lit in its current state
     */
    @Nullable
    BlockState execute(Block block);
}
