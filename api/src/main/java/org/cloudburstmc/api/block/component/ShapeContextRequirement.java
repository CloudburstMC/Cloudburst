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
     * Block-entity state may affect the shape.
     */
    BLOCK_ENTITY
}
