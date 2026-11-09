package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A cluster of turtle eggs sharing a hatch stage.
 */
public interface TurtleEgg extends Hatchable {

    /**
     * @return the number of eggs
     */
    int getEggs();

    /**
     * @return the smallest supported egg count
     */
    int getMinimumEggs();

    /**
     * @return the greatest supported egg count
     */
    int getMaximumEggs();

    /**
     * @param eggs the replacement count within the supported bounds
     * @return a state with the requested number of eggs
     * @throws IllegalArgumentException if the count is outside the supported range
     */
    BlockState withEggs(int eggs);
}
