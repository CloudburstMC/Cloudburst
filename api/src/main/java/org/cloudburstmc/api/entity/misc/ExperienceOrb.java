package org.cloudburstmc.api.entity.misc;

import org.checkerframework.checker.index.qual.NonNegative;
import org.cloudburstmc.api.entity.Entity;

/**
 * A single orb representing one or more experience pickups of equal value.
 */
public interface ExperienceOrb extends Entity {

    /**
     * Returns the experience value of one pickup, not the total for the stack.
     */
    @NonNegative
    int getExperience();

    /**
     * Sets the experience value of one pickup, including zero.
     *
     * @throws IllegalArgumentException if experience is negative
     */
    void setExperience(@NonNegative int experience);

    /**
     * Returns the number of pickups remaining in this orb.
     */
    int getCount();

    /**
     * Sets how many times this orb can be picked up before it disappears.
     *
     * @throws IllegalArgumentException if the count is less than one
     */
    void setCount(int count);

    /**
     * Returns the remaining pickup delay in ticks, or 32767 if pickup is disabled.
     */
    @NonNegative
    int getPickupDelay();

    /**
     * Sets the pickup delay in ticks. A value of 32767 or more prevents pickup indefinitely.
     */
    void setPickupDelay(@NonNegative int pickupDelay);
}
