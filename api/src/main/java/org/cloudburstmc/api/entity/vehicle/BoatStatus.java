package org.cloudburstmc.api.entity.vehicle;

/**
 * The medium surrounding or supporting a boat.
 */
public enum BoatStatus {
    /**
     * Neither floating in water nor supported by a block.
     */
    IN_AIR,
    /**
     * Floating at the water surface.
     */
    IN_WATER,
    /**
     * Supported by a block's collision shape.
     */
    ON_LAND,
    /**
     * Submerged beneath flowing water.
     */
    UNDER_FLOWING_WATER,
    /**
     * Submerged beneath source water.
     */
    UNDER_WATER
}
