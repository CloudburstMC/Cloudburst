package org.cloudburstmc.api.util;

/**
 * Source of an entity movement.
 */
public enum MovementType {
    /**
     * Movement caused by the entity itself.
     */
    SELF,
    /**
     * Movement caused by player input.
     */
    PLAYER,
    /**
     * Movement caused by a piston.
     */
    PISTON,
    /**
     * Movement caused by a shulker.
     */
    SHULKER,
    /**
     * Movement caused by a shulker box.
     */
    SHULKER_BOX
}
