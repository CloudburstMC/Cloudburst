package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.BlockState;

/**
 * Block and light access within a locked chunk neighborhood.
 * Coordinates are absolute block coordinates. Absent sections and out-of-height
 * positions read as air or zero. Writes outside available chunks or height limits are ignored.
 */
public interface LightAccess {

    /**
     * Returns the lowest accessible y.
     */
    int minHeight();

    /**
     * Returns one past the highest accessible y.
     */
    int maxHeight();

    /**
     * Returns the block-state cache used by this access.
     */
    LightStateCache states();

    /**
     * Returns whether the chunk containing these block coordinates is available.
     */
    boolean isLoaded(int x, int z);

    /**
     * Returns the primary-layer block state.
     */
    BlockState primary(int x, int y, int z);

    /**
     * Returns the secondary-layer block state.
     */
    BlockState secondary(int x, int y, int z);

    /**
     * Returns the current value of this access's light channel.
     */
    int light(int x, int y, int z);

    /**
     * Writes a value from 0 to 15 to this access's light channel.
     */
    void setLight(int x, int y, int z, int light);
}
