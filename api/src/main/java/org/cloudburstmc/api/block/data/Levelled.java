package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A fluid depth or container fill level using the block type's own scale.
 * Fluid level zero is a source, one through seven are horizontal flow depths,
 * and eight through fifteen are falling fluid depths.
 */
public interface Levelled extends BlockData {

    /**
     * @return the current level
     */
    int getLevel();

    /**
     * @return the lowest supported level
     */
    int getMinimumLevel();

    /**
     * @return the highest supported level
     */
    int getMaximumLevel();

    /**
     * @param level the replacement level within the supported bounds
     * @return a state with the requested level
     * @throws IllegalArgumentException if the level is outside the supported range
     */
    BlockState withLevel(int level);
}
