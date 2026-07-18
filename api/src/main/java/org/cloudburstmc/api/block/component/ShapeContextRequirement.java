package org.cloudburstmc.api.block.component;

/**
 * External state required to calculate a block shape.
 */
public enum ShapeContextRequirement {
    /**
     * The immutable block state completely determines the shape.
     */
    STATE_ONLY,
    /**
     * The surrounding level may affect the shape.
     */
    LEVEL,
    /**
     * The colliding entity may affect the shape.
     */
    ENTITY,
    /**
     * Block-entity state may affect the shape.
     */
    BLOCK_ENTITY
}
