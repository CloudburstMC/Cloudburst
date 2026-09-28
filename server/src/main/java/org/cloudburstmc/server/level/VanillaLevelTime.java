package org.cloudburstmc.server.level;

import lombok.experimental.UtilityClass;

/**
 * Defines the vanilla daylight-cycle markers used by server mechanics and commands.
 */
@UtilityClass
public class VanillaLevelTime {
    public static final long DAY = 0;
    public static final long NOON = 6000;
    public static final long SUNSET = 12000;
    public static final long NIGHT = 14000;
    public static final long MIDNIGHT = 18000;
    public static final long SUNRISE = 23000;
    public static final long TICKS_PER_DAY = 24000;

    private static final int MOON_PHASE_COUNT = 8;
    private static final long NETWORK_CYCLE_TICKS = TICKS_PER_DAY * MOON_PHASE_COUNT;

    public static boolean isNight(long time) {
        long timeOfDay = Math.floorMod(time, TICKS_PER_DAY);
        return timeOfDay >= NIGHT && timeOfDay < SUNRISE;
    }

    public static long startOfNextDay(long time) {
        long timeOfDay = Math.floorMod(time, TICKS_PER_DAY);
        return time + TICKS_PER_DAY - timeOfDay;
    }

    public static int toNetworkTime(long time) {
        return (int) Math.floorMod(time, NETWORK_CYCLE_TICKS);
    }
}
