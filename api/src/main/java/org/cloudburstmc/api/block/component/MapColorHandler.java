package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;

import java.awt.Color;

/**
 * Determines the map color of a block.
 */
@FunctionalInterface
public interface MapColorHandler {

    /**
     * @param block the block to query
     * @return the map color
     */
    Color execute(Block block);
}
