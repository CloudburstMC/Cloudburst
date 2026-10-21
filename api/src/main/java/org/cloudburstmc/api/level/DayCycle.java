package org.cloudburstmc.api.level;

import lombok.experimental.UtilityClass;

/**
 * Daylight-cycle markers and tick conversions.
 */
@UtilityClass
public class DayCycle {

    /**
     * Number of ticks in one complete daylight cycle.
     */
    public static final int TICKS_PER_DAY = 24000;

    /**
     * Morning time selected by the day marker.
     */
    public static final int DAY = 1000;

    /**
     * Midday within a daylight cycle.
     */
    public static final int NOON = 6000;

    /**
     * Sunset within a daylight cycle.
     */
    public static final int SUNSET = 12000;

    /**
     * Nighttime selected by the night marker.
     */
    public static final int NIGHT = 13000;

    /**
     * Midnight within a daylight cycle.
     */
    public static final int MIDNIGHT = 18000;

    /**
     * Sunrise within a daylight cycle.
     */
    public static final int SUNRISE = 23000;

    /**
     * Returns the position within a day.
     *
     * @param fullTime the full daylight time
     * @return a tick from 0 through 23999
     */
    public static int timeOfDay(long fullTime) {
        return Math.floorMod(fullTime, TICKS_PER_DAY);
    }

    /**
     * Returns the number of complete days.
     *
     * @param fullTime the full daylight time
     * @return the day count
     */
    public static long day(long fullTime) {
        return Math.floorDiv(fullTime, TICKS_PER_DAY);
    }

    /**
     * Resolves a relative time without moving the clock backward.
     * An already matching time leaves the clock unchanged.
     *
     * @param fullTime the current full daylight time
     * @param time     the desired relative time
     * @return the next matching full daylight time
     * @throws ArithmeticException if the result exceeds the supported tick range
     */
    public static long atOrAfter(long fullTime, long time) {
        return Math.addExact(fullTime, Math.floorMod(timeOfDay(time) - timeOfDay(fullTime), TICKS_PER_DAY));
    }
}
