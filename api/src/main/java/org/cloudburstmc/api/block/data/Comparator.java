package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.data.ComparatorMode;

/**
 * A redstone comparator's facing, powered state and comparison mode.
 */
public interface Comparator extends Directional, Powerable {

    /**
     * @return the comparison mode
     */
    ComparatorMode getMode();

    /**
     * @param mode the replacement comparison mode
     * @return a state using the requested mode
     */
    BlockState withMode(ComparatorMode mode);
}
