package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A beehive or bee nest's facing and stored honey level.
 */
public interface Beehive extends Directional {

    /**
     * @return the current honey level
     */
    int getHoneyLevel();

    /**
     * @return the greatest supported honey level
     */
    int getMaximumHoneyLevel();

    /**
     * @param honeyLevel the replacement level, from zero through {@link #getMaximumHoneyLevel()}
     * @return a state with the requested honey level
     * @throws IllegalArgumentException if the level is outside the supported range
     */
    BlockState withHoneyLevel(int honeyLevel);
}
