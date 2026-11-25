package org.cloudburstmc.api.blockentity;

/**
 * A daylight sensor with normal and inverted modes.
 */
public interface DaylightDetector extends BlockEntity {

    /**
     * Returns whether the sensor is in inverted mode.
     */
    boolean isInverted();

    /**
     * Selects the sensor's mode.
     *
     * @param inverted whether to select inverted mode
     */
    void setInverted(boolean inverted);
}
