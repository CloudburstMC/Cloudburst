package org.cloudburstmc.api.level;

/**
 * The eight phases of the lunar cycle, starting with a full moon on day zero.
 */
public enum MoonPhase {
    FULL_MOON,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT,
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS;

    /**
     * Resolves the moon phase for a day count.
     *
     * @param day the number of complete days
     * @return the moon phase
     */
    public static MoonPhase forDay(long day) {
        MoonPhase[] phases = values();
        return phases[Math.floorMod(day, phases.length)];
    }
}
