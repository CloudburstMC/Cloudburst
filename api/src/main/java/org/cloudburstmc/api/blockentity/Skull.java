package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.util.data.CardinalDirection;

/**
 * A placed head or skull. Its block type identifies the head it depicts.
 */
public interface Skull extends BlockEntity {

    /**
     * Returns the direction the head faces.
     *
     * @return the head's orientation
     */
    CardinalDirection getRotation();

    /**
     * Sets the rotation of a floor-mounted head. Wall-mounted heads face
     * the direction of their block state.
     *
     * @param rotation the new orientation
     * @throws IllegalStateException if the head is wall-mounted
     */
    void setRotation(CardinalDirection rotation);
}
