package org.cloudburstmc.api.util.data;

/**
 * The tilt of a big dripleaf's leaf.
 */
public enum DripleafTilt {
    /**
     * Upright and able to support entities.
     */
    NONE,
    /**
     * Preparing to tilt while retaining its full supporting surface.
     */
    UNSTABLE,
    /**
     * Partly tilted with a lowered supporting surface.
     */
    PARTIAL_TILT,
    /**
     * Fully tilted and unable to support entities.
     */
    FULL_TILT
}
