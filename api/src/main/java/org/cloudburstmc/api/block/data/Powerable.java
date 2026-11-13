package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A block with a powered state.
 */
public interface Powerable extends BlockData {

    /**
     * @return whether the block is powered
     */
    boolean isPowered();

    /**
     * For blocks with a numeric signal, enabling power retains a positive
     * strength or selects the maximum when currently unpowered. Disabling power
     * sets the strength to zero.
     *
     * @param powered whether the resulting state is powered
     * @return a state with the requested powered value
     */
    BlockState withPowered(boolean powered);
}
