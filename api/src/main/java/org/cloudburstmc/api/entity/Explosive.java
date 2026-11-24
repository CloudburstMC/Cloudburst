package org.cloudburstmc.api.entity;

/**
 * An entity with configurable explosion settings.
 */
public interface Explosive extends Entity {

    /**
     * Returns the base explosion radius in blocks.
     */
    float getYield();

    /**
     * Sets the base explosion radius in blocks.
     *
     * @throws IllegalArgumentException if the radius is negative or not finite
     */
    void setYield(float yield);

    /**
     * Returns whether this entity's explosions may start fires.
     */
    boolean isIncendiary();

    /**
     * Sets whether this entity's explosions may start fires.
     */
    void setIsIncendiary(boolean incendiary);
}
