package org.cloudburstmc.api.entity.passive;

import org.cloudburstmc.api.entity.Mob;

/**
 * An aquatic mammal with a moisture timer.
 */
public interface Dolphin extends Mob {

    /**
     * Returns the number of dry ticks remaining before dehydration damage starts.
     */
    int getMoistness();

    /**
     * Sets the number of dry ticks remaining before dehydration damage starts.
     *
     * @throws IllegalArgumentException if the value is negative
     */
    void setMoistness(int moistness);
}
