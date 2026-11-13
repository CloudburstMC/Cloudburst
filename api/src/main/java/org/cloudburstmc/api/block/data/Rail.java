package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.data.RailDirection;

import java.util.Set;

/**
 * The straight, ascending or curved layout of a minecart rail.
 */
public interface Rail extends BlockData {

    /**
     * @return the current rail shape
     */
    RailDirection getShape();

    /**
     * @return the immutable set of shapes supported by this rail
     */
    Set<RailDirection> getShapes();

    /**
     * @param shape the replacement rail shape
     * @return a state with the requested shape
     * @throws IllegalArgumentException if the shape is not supported
     */
    BlockState withShape(RailDirection shape);
}
