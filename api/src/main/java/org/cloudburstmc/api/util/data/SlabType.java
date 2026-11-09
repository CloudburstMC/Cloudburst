package org.cloudburstmc.api.util.data;

/**
 * The part of a block space occupied by a slab.
 */
public enum SlabType {
    /**
     * The lower half.
     */
    BOTTOM,
    /**
     * The upper half.
     */
    TOP,
    /**
     * The entire block space.
     */
    DOUBLE
}
