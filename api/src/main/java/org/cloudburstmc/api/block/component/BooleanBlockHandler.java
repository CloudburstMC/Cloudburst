package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

/**
 * Evaluates a boolean property of a block.
 */
@FunctionalInterface
public interface BooleanBlockHandler {

    /**
     * @param block the block to evaluate
     * @return the property value
     */
    boolean execute(Block block);
}
