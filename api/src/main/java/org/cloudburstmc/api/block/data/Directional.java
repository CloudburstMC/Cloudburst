package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;

import java.util.Set;

/**
 * A block with a facing direction.
 */
public interface Directional extends BlockData {

    /**
     * @return the current facing direction
     */
    Direction getFacing();

    /**
     * @return the immutable set of supported facing directions
     */
    Set<Direction> getFaces();

    /**
     * @param facing the replacement direction
     * @return a state with the requested direction
     * @throws IllegalArgumentException if the direction is not supported
     */
    BlockState withFacing(Direction facing);
}
