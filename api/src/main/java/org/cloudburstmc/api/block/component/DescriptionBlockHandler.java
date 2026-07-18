package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.BlockState;

/**
 * Determines the description identifier of a block state.
 */
@FunctionalInterface
public interface DescriptionBlockHandler {

    /**
     * @param state the block state to query
     * @return the description identifier
     */
    String execute(BlockState state);
}
