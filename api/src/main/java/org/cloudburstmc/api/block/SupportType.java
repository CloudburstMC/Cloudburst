package org.cloudburstmc.api.block;

/**
 * Type of support required from a block face.
 */
public enum SupportType {
    /**
     * The full face must be supported.
     */
    FULL,
    /**
     * The center of the face must be supported.
     */
    CENTER,
    /**
     * The outer face, excluding its central column, must be supported.
     */
    RIGID
}
