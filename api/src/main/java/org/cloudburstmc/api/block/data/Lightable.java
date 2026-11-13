package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A block with a lit state.
 */
public interface Lightable extends BlockData {

    /**
     * @return whether the block is lit
     */
    boolean isLit();

    /**
     * Returns a state with the requested lighting, preserving other shared
     * properties. The resulting block type may differ from the original type.
     *
     * @param lit whether the resulting state is lit
     * @return a state with the requested lit value
     */
    BlockState withLit(boolean lit);
}
