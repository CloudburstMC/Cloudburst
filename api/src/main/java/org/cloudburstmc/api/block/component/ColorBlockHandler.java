package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

/**
 * Determines the color associated with a block.
 */
@FunctionalInterface
public interface ColorBlockHandler {

    /**
     * @param block the block to query
     * @return the block color
     */
    BlockColor execute(Block block);
}
