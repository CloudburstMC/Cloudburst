package org.cloudburstmc.api.util.data;

/**
 * The surface on which a block is mounted.
 */
public enum AttachedFace {
    /**
     * Mounted on a surface below the block.
     */
    FLOOR,
    /**
     * Mounted on a horizontal neighbor's surface.
     */
    WALL,
    /**
     * Mounted on a surface above the block.
     */
    CEILING
}
