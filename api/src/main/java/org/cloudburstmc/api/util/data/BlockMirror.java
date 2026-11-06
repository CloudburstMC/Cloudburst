package org.cloudburstmc.api.util.data;

/**
 * A reflection across a vertical plane through the block's center.
 */
public enum BlockMirror {
    /**
     * Leaves the orientation unchanged.
     */
    NONE,
    /**
     * Exchanges north and south while retaining east and west.
     */
    LEFT_RIGHT,
    /**
     * Exchanges east and west while retaining north and south.
     */
    FRONT_BACK
}
