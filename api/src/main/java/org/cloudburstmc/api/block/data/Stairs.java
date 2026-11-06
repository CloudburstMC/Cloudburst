package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.data.StairShape;

/**
 * Stairs with a facing direction, vertical half and corner shape.
 */
public interface Stairs extends Directional, Bisected {

    /**
     * @return the current corner shape
     */
    StairShape getShape();

    /**
     * @param shape the replacement corner shape
     * @return a state with the requested shape
     */
    BlockState withShape(StairShape shape);
}
