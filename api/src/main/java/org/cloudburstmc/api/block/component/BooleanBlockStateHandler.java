package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;

/**
 * Evaluates a boolean property of a block state.
 */
@FunctionalInterface
public interface BooleanBlockStateHandler {

    /**
     * @param state the block state to evaluate
     * @return the property value
     */
    boolean execute(BlockState state);
}
