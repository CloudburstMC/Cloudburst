package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.Direction;

/**
 * A piece of artwork attached to a wall.
 */
public interface Painting extends Entity {

    /**
     * Returns the artwork displayed by this painting.
     */
    PaintingVariant getVariant();

    /**
     * Changes the artwork displayed by this painting.
     */
    void setVariant(PaintingVariant variant);

    /**
     * Returns the horizontal direction the front of this painting faces.
     */
    Direction getFacing();
}
