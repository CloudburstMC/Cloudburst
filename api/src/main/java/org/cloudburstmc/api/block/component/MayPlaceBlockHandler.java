package org.cloudburstmc.api.block.component;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.Direction;

/**
 * Determines whether a block may be placed against a face.
 */
@FunctionalInterface
public interface MayPlaceBlockHandler {

    /**
     * @param block the block being placed
     * @param direction the supporting face
     * @return whether placement is allowed
     */
    boolean execute(Block block, Direction direction);
}
