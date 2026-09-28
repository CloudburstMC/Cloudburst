package org.cloudburstmc.api.level;

/**
 * Selects the block geometry used by a level ray trace.
 */
public enum BlockShapeMode {
    /**
     * Physical collision geometry, suitable for moving entities and projectiles.
     */
    COLLIDER,
    /**
     * Selection and interaction outline, suitable for block targeting.
     */
    OUTLINE
}
