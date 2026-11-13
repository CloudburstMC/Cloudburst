package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A block whose state distinguishes hanging placement.
 */
public interface Hangable extends BlockData {

    /**
     * @return whether the block uses its hanging form
     */
    boolean isHanging();

    /**
     * @param hanging whether to use the hanging form
     * @return a state with the requested form
     */
    BlockState withHanging(boolean hanging);
}
