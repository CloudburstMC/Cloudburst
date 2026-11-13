package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;

import java.util.Set;

/**
 * A block oriented along an axis.
 */
public interface Orientable extends BlockData {

    /**
     * @return the current axis
     */
    Direction.Axis getAxis();

    /**
     * @return the immutable set of supported axes
     */
    Set<Direction.Axis> getAxes();

    /**
     * @param axis the replacement axis
     * @return a state with the requested axis
     * @throws IllegalArgumentException if the axis is not supported
     */
    BlockState withAxis(Direction.Axis axis);
}
