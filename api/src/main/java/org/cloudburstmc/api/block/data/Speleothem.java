package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.DripstoneThickness;

import java.util.Set;

/**
 * The vertical direction and segment shape of pointed dripstone, sulfur spikes or icicles.
 */
public interface Speleothem extends BlockData {

    /**
     * @return the direction toward the tip
     */
    Direction getVerticalDirection();

    /**
     * @return the immutable set of supported tip directions
     */
    Set<Direction> getVerticalDirections();

    /**
     * @param direction the replacement tip direction
     * @return a state pointing in the requested direction
     * @throws IllegalArgumentException if the direction is not supported
     */
    BlockState withVerticalDirection(Direction direction);

    /**
     * @return the segment's thickness or tip shape
     */
    DripstoneThickness getThickness();

    /**
     * @param thickness the replacement segment shape
     * @return a state with the requested shape
     */
    BlockState withThickness(DripstoneThickness thickness);
}
