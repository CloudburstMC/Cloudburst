package org.cloudburstmc.server.network;

import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.MoonPhase;
import org.cloudburstmc.api.level.WorldClock;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.server.level.CloudLevelData;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        WorldClock clock = InterfaceProxy.create(WorldClock.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getTotalTicks" -> data.getClock().getTotalTicks();
            case "isPaused" -> data.getClock().isPaused();
            case "getRate" -> data.getClock().getRate();
            default -> throw new AssertionError("Unexpected clock access: " + method);
        });
        Level level = InterfaceProxy.create(Level.class, Map.of("getDefaultClock", clock, "getGameRules", data.getGameRules()));
        Player player = InterfaceProxy.create(Player.class, Map.of(
                "getLevel", level, "getPlayerTime", 49000L, "isPlayerTimeRelative", true));

        assertTrue(LevelTimePacketFactory.advancesAutomatically(player));
        assertEquals(49000, LevelTimePacketFactory.time(player).getTime());

        data.getClock().setRate(0.5f);
        assertFalse(LevelTimePacketFactory.advancesAutomatically(player));
        assertFalse(daylightRule(player));
        assertTrue(data.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE));

        data.getClock().setRate(1);
        data.getClock().setPaused(true);
        assertFalse(daylightRule(player));

        data.getClock().setPaused(false);
        assertTrue(daylightRule(player));

        data.getGameRules().set(GameRules.DO_DAYLIGHT_CYCLE, false);
        assertFalse(daylightRule(player));
    }

    @Test
    void fixedDisplayTimeDoesNotPauseOtherPlayersOrTheLevel() {
        CloudLevelData data = new CloudLevelData();
        data.getGameRules().set(GameRules.DO_DAYLIGHT_CYCLE, true);
        WorldClock clock = InterfaceProxy.create(WorldClock.class, Map.of("getRate", 1f));
        Level level = InterfaceProxy.create(Level.class, Map.of("getDefaultClock", clock, "getGameRules", data.getGameRules()));
        Player fixed = InterfaceProxy.create(Player.class, Map.of("getLevel", level, "getPlayerTime", 18000L));
        Player relative = InterfaceProxy.create(Player.class, Map.of("getLevel", level, "isPlayerTimeRelative", true));

        assertEquals(18000, LevelTimePacketFactory.time(fixed).getTime());
        assertFalse(daylightRule(fixed));
        assertTrue(daylightRule(relative));
        assertTrue(data.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE));
    }

    private static boolean daylightRule(Player player) {
        List<GameRuleData<?>> matches = LevelTimePacketFactory.gameRules(player).getGameRules().stream()
                .filter(rule -> rule.getName().equals(GameRules.DO_DAYLIGHT_CYCLE.getName()))
                .toList();
        assertEquals(1, matches.size());
        return (Boolean) matches.getFirst().getValue();
    }
}
