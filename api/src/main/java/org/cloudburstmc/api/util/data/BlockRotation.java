package org.cloudburstmc.api.util.data;

/**
 * A horizontal rotation around the vertical axis, viewed from above.
 */
public enum BlockRotation {
    /**
     * Leaves the orientation unchanged.
     */
    NONE,
    /**
     * Turns north toward east by one quarter turn.
     */
    CLOCKWISE_90,
    /**
     * Turns each horizontal direction toward its opposite.
     */
    CLOCKWISE_180,
    /**
     * Turns north toward west by one quarter turn.
     */
    COUNTERCLOCKWISE_90
}
