package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * An egg's progress toward hatching, independent of the number of eggs present.
 */
public interface Hatchable extends BlockData {

    /**
     * @return the current hatch stage, starting at zero
     */
    int getHatch();

    /**
     * @return the greatest supported hatch stage
     */
    int getMaximumHatch();

    /**
     * @param hatch the replacement stage, from zero through {@link #getMaximumHatch()}
     * @return a state with the requested hatch stage
     * @throws IllegalArgumentException if the stage is outside the supported range
     */
    BlockState withHatch(int hatch);
}
