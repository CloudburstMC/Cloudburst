package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A block of layered snow.
 */
public interface Snow extends BlockData {

    /**
     * @return the number of snow layers
     */
    int getLayers();

    /**
     * @return the smallest supported layer count
     */
    int getMinimumLayers();

    /**
     * @return the greatest supported layer count
     */
    int getMaximumLayers();

    /**
     * @param layers the replacement count within the supported bounds
     * @return a state with the requested number of snow layers
     * @throws IllegalArgumentException if the count is outside the supported range
     */
    BlockState withLayers(int layers);
}
