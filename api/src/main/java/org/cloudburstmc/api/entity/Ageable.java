package org.cloudburstmc.api.entity;

/**
 * A mob with a growth or breeding timer.
 */
public interface Ageable extends Mob {

    /**
     * Returns the age in ticks. Negative values count toward adulthood;
     * positive values count down a cooldown. Zero means neither timer is active.
     */
    int getAge();

    /**
     * Sets the age in ticks. A negative age makes the mob juvenile.
     */
    void setAge(int age);
}
