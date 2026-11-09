package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * Farmland's moisture, from zero for dry soil to its fully hydrated maximum.
 */
public interface Farmland extends BlockData {

    /**
     * @return the current moisture
     */
    int getMoisture();

    /**
     * @return the greatest supported moisture
     */
    int getMaximumMoisture();

    /**
     * @param moisture the replacement moisture, from zero through {@link #getMaximumMoisture()}
     * @return a state with the requested moisture
     * @throws IllegalArgumentException if the moisture is outside the supported range
     */
    BlockState withMoisture(int moisture);
}
