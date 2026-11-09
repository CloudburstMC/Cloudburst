package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A block with a numeric redstone signal strength.
 */
public interface AnaloguePowerable extends BlockData {

    /**
     * @return the current signal strength
     */
    int getPower();

    /**
     * @return the greatest supported signal strength
     */
    int getMaximumPower();

    /**
     * @param power the replacement strength, from zero through {@link #getMaximumPower()}
     * @return a state with the requested signal strength
     * @throws IllegalArgumentException if the strength is outside the supported range
     */
    BlockState withPower(int power);
}
