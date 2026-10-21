package org.cloudburstmc.server.network;

import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.MoonPhase;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LevelTimePacketFactoryTest {

    @Test
    void networkTimePreservesTheWholeLunarCycle() {
        for (long day = -9; day <= 9; day++) {
            long fullTime = day * DayCycle.TICKS_PER_DAY + DayCycle.NOON;
            int networkTime = LevelTimePacketFactory.networkTime(fullTime);

            assertEquals(DayCycle.NOON, DayCycle.timeOfDay(networkTime));
            assertEquals(MoonPhase.forDay(day), MoonPhase.forDay(DayCycle.day(networkTime)));
            assertTrue(networkTime >= 0 && networkTime < 8 * DayCycle.TICKS_PER_DAY);
        }
    }

    @Test
    void customRatesAndPausesDisableExtrapolationWithoutChangingTheServerRule() {
        CloudLevelData data = new CloudLevelData();
        data.getGameRules().set(GameRules.DO_DAYLIGHT_CYCLE, true);
        data.getClock().setTotalTicks(49000);
        Level level = InterfaceProxy.create(Level.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "hasDaylightCycle" -> true;
            case "getFullTime" -> data.getClock().getTotalTicks();
            case "isTimePaused" -> data.getClock().isPaused();
            case "getTimeRate" -> data.getClock().getRate();
            case "getGameRules" -> data.getGameRules();
            default -> throw new AssertionError("Unexpected level access: " + method);
        });

        assertTrue(LevelTimePacketFactory.advancesAutomatically(level));
        assertEquals(49000, LevelTimePacketFactory.time(level).getTime());

        data.getClock().setRate(0.5f);
        assertFalse(LevelTimePacketFactory.advancesAutomatically(level));
        assertFalse(daylightRule(level));
        assertTrue(data.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE));

        data.getClock().setRate(1);
        data.getClock().setPaused(true);
        assertFalse(daylightRule(level));

        data.getClock().setPaused(false);
        assertTrue(daylightRule(level));

        data.getGameRules().set(GameRules.DO_DAYLIGHT_CYCLE, false);
        assertFalse(daylightRule(level));
    }

    private static boolean daylightRule(Level level) {
        List<GameRuleData<?>> matches = LevelTimePacketFactory.gameRules(level).getGameRules().stream()
                .filter(rule -> rule.getName().equals(GameRules.DO_DAYLIGHT_CYCLE.getName()))
                .toList();
        assertEquals(1, matches.size());
        return (Boolean) matches.getFirst().getValue();
    }
}
