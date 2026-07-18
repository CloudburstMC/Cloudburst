package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;

/**
 * Calculates an integer property of a block state.
 */
@FunctionalInterface
public interface IntBlockHandler {

    /**
     * @param state the block state to query
     * @return the property value
     */
    int execute(BlockState state);
}
