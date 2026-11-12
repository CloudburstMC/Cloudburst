package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

/**
 * Reads a block's analog output for a comparator.
 */
@FunctionalInterface
public interface ComparatorOutputHandler {

    /**
     * Returns a signal from 0 to 15, or -1 if the block has no analog output.
     */
    int execute(Block block);
}
