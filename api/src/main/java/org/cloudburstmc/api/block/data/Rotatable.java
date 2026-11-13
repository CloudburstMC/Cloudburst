package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.data.CardinalDirection;

import java.util.Set;

/**
 * A block with a horizontal rotation, including intermediate compass points.
 */
public interface Rotatable extends BlockData {

    /**
     * @return the current rotation
     */
    CardinalDirection getRotation();

    /**
     * @return the immutable set of supported rotations
     */
    Set<CardinalDirection> getRotations();

    /**
     * @param rotation the replacement rotation
     * @return a state with the requested rotation
     * @throws IllegalArgumentException if the rotation is not supported
     */
    BlockState withRotation(CardinalDirection rotation);
}
