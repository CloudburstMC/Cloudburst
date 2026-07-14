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
     * A rigid central area must be supported.
     */
    RIGID
}
