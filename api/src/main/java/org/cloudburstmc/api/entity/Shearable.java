package org.cloudburstmc.api.entity;

/**
 * An entity that can be sheared.
 */
public interface Shearable extends Entity {

    /**
     * Returns whether this entity can currently be sheared.
     */
    boolean readyForShearing();

    /**
     * Shears this entity using its default drops when it is ready, without a player interaction.
     *
     * @return whether the entity was sheared
     */
    boolean shear();
}
