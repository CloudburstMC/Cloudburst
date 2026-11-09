package org.cloudburstmc.api.util.data;

/**
 * Shape of a redstone wire connection in one horizontal direction.
 */
public enum RedstoneSide {
    /**
     * No connection in this direction.
     */
    NONE,
    /**
     * A connection along the supporting surface.
     */
    SIDE,
    /**
     * A connection extending up the side of the neighboring block.
     */
    UP
}
