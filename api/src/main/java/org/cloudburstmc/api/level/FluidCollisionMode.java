package org.cloudburstmc.api.level;

/**
 * Selects which liquids can stop a level ray trace.
 */
public enum FluidCollisionMode {
    /**
     * Ignore all liquids.
     */
    NONE,
    /**
     * Hit only source liquid states.
     */
    SOURCE_ONLY,
    /**
     * Hit source and flowing liquid states.
     */
    ANY,
    /**
     * Hit source and flowing water, but not lava.
     */
    WATER
}
