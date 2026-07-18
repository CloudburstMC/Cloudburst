package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;

/**
 * Calculates a floating-point property of a block state.
 */
@FunctionalInterface
public interface FloatBlockHandler {

    /**
     * @param state the block state to query
     * @return the property value
     */
    float execute(BlockState state);
}
