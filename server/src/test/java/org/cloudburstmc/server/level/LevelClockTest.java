package org.cloudburstmc.server.level;

import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.api.level.MoonPhase;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevelClockTest {

    @Test
    void gameTimeKeepsAdvancingWhenTheDaylightClockStops() {
        CloudLevelData data = new CloudLevelData();
        data.getGameRules().set(GameRules.DO_DAYLIGHT_CYCLE, true);
        data.getClock().setTotalTicks(6000);
        data.getClock().setPaused(true);

        data.tick();

        assertEquals(1, data.getGameTime());
        assertEquals(6000, data.getClock().getTotalTicks());

        data.getClock().setPaused(false);
        data.getGameRules().set(GameRules.DO_DAYLIGHT_CYCLE, false);
        data.tick();

        assertEquals(2, data.getGameTime());
        assertEquals(6000, data.getClock().getTotalTicks());

        data.getGameRules().set(GameRules.DO_DAYLIGHT_CYCLE, true);
        data.tick();

        assertEquals(3, data.getGameTime());
        assertEquals(6001, data.getClock().getTotalTicks());
    }

    @Test
    void fractionalRatesAccumulateAndPausePreservesTheirRemainder() {
        LevelClock clock = new LevelClock();
        clock.setRate(0.5f);
        clock.tick();

        assertEquals(0, clock.getTotalTicks());
        assertEquals(0.5f, clock.getPartialTick());

        clock.setPaused(true);
        clock.tick();
        assertEquals(0.5f, clock.getPartialTick());

        clock.setPaused(false);
        clock.tick();
        assertEquals(1, clock.getTotalTicks());
        assertEquals(0, clock.getPartialTick());

        clock.setRate(2.5f);
        clock.tick();
        clock.tick();
        assertEquals(6, clock.getTotalTicks());

        clock.setPartialTick(0.75f);
        clock.setTotalTicks(12000);
        assertEquals(0, clock.getPartialTick());
    }

    @Test
    void copiesDoNotShareMutableClockState() {
        CloudLevelData source = new CloudLevelData();
        source.setGameTime(50);
        source.getClock().setTotalTicks(49000);
        source.getClock().setRate(0.5f);
        source.getClock().setPartialTick(0.25f);
        source.getClock().setPaused(true);

        CloudLevelData copy = source.copy();
        copy.getClock().setTotalTicks(1);
        copy.getClock().setPaused(false);

        assertEquals(49000, source.getClock().getTotalTicks());
        assertEquals(0.25f, source.getClock().getPartialTick());
        assertTrue(source.getClock().isPaused());
        assertEquals(0.5f, copy.getClock().getRate());
        assertEquals(50, copy.getGameTime());
    }

    @Test
    void fixedDimensionsDoNotAdvanceADaylightClock() {
        for (int dimension : new int[]{CloudLevel.DIMENSION_NETHER, CloudLevel.DIMENSION_THE_END}) {
            CloudLevelData data = new CloudLevelData();
            data.setDimension(dimension);
            data.getGameRules().set(GameRules.DO_DAYLIGHT_CYCLE, true);

            data.tick();

            assertEquals(1, data.getGameTime());
            assertEquals(0, data.getClock().getTotalTicks());
        }
    }

    @Test
    void dayConversionsPreserveDayCountsAndHandleNegativeTimes() {
        assertEquals(49000, DayCycle.atOrAfter(48000, DayCycle.DAY));
        assertEquals(73000, DayCycle.atOrAfter(50000, DayCycle.DAY));
        assertEquals(49000, DayCycle.atOrAfter(49000, DayCycle.DAY));
        assertEquals(23999, DayCycle.timeOfDay(-1));
        assertEquals(-1, DayCycle.day(-1));
        assertEquals(MoonPhase.WAXING_GIBBOUS, MoonPhase.forDay(-1));
        assertEquals(MoonPhase.FULL_MOON, MoonPhase.forDay(8));
    }

    @Test
    void ratesAndRemaindersRejectInvalidNumbers() {
        LevelClock clock = new LevelClock();
        for (float rate : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY, 1001}) {
            assertThrows(IllegalArgumentException.class, () -> clock.setRate(rate));
        }
        for (float remainder : new float[]{-1, 1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> clock.setPartialTick(remainder));
        }
    }
}
