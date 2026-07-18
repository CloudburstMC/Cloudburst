package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

/**
 * Performs an operation on a block.
 */
@FunctionalInterface
public interface ComplexBlockHandler {

    /**
     * @param block the block to process
     */
    void execute(Block block);
}
