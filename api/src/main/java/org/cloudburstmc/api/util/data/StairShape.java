package org.cloudburstmc.api.util.data;

/**
 * Describes how a stair block joins adjacent stairs.
 */
public enum StairShape {
    /**
     * Steps without a corner.
     */
    STRAIGHT,
    /**
     * An inner left-hand corner.
     */
    INNER_LEFT,
    /**
     * An inner right-hand corner.
     */
    INNER_RIGHT,
    /**
     * An outer left-hand corner.
     */
    OUTER_LEFT,
    /**
     * An outer right-hand corner.
     */
    OUTER_RIGHT
}
