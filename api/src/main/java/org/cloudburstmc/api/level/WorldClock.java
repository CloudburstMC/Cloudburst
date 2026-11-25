package org.cloudburstmc.api.level;

import org.cloudburstmc.api.event.level.TimeSkipEvent;

/**
 * The daylight clock of a level, independent of elapsed simulation ticks.
 */
public interface WorldClock {

    /**
     * Returns the level that owns this clock.
     */
    Level getLevel();

    /**
     * Returns the daylight tick count, including completed days.
     */
    long getTotalTicks();

    /**
     * Sets the daylight tick count, subject to {@link TimeSkipEvent}.
     *
     * @param ticks the new count
     * @return whether the change was accepted
     */
    boolean setTotalTicks(long ticks);

    /**
     * Returns the position within the current day, from 0 through 23999.
     */
    default int getTimeOfDay() {
        return DayCycle.timeOfDay(this.getTotalTicks());
    }

    /**
     * Moves forward to the requested time within a day.
     *
     * @param time the desired time, wrapped to one day
     * @return whether the change was accepted
     */
    default boolean setTimeOfDay(long time) {
        return this.setTotalTicks(DayCycle.atOrAfter(this.getTotalTicks(), time));
    }

    /**
     * Returns whether this clock is explicitly paused.
     */
    boolean isPaused();

    /**
     * Pauses or resumes this clock without changing the daylight cycle game rule.
     */
    void setPaused(boolean paused);

    /**
     * Returns the number of daylight ticks per simulation tick.
     */
    float getRate();

    /**
     * Sets the number of daylight ticks per simulation tick.
     *
     * @param rate a finite value from 0.00001 through 1000
     * @throws IllegalArgumentException if the rate is outside the supported range
     */
    void setRate(float rate);
}
