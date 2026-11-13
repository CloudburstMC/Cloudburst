package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A block with an attachment state, such as connected tripwire or an attached hanging sign.
 */
public interface Attachable extends BlockData {

    /**
     * @return whether the block is attached
     */
    boolean isAttached();

    /**
     * @param attached the replacement attachment state
     * @return a state with the requested attachment
     */
    BlockState withAttached(boolean attached);
}
