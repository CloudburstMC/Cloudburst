package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.api.level.WorldClock;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket;

import java.util.List;

@UtilityClass
public class LevelTimePacketFactory {

    private static final long LUNAR_CYCLE_TICKS = DayCycle.TICKS_PER_DAY * 8L;

    public static int networkTime(long fullTime) {
        return (int) Math.floorMod(fullTime, LUNAR_CYCLE_TICKS);
    }

    public static SetTimePacket time(Player player) {
        SetTimePacket packet = new SetTimePacket();
        packet.setTime(networkTime(player.getPlayerTime()));
        return packet;
    }

    public static boolean advancesAutomatically(Player player) {
        WorldClock clock = player.getLevel().getDefaultClock();
        return clock != null && player.isPlayerTimeRelative() && !clock.isPaused() && clock.getRate() == 1 && player.getLevel().getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE);
    }

    public static GameRulesChangedPacket gameRules(Player player) {
        GameRulesChangedPacket packet = new GameRulesChangedPacket();
        writeGameRules(player, packet.getGameRules());
        return packet;
    }

    public static GameRulesChangedPacket daylightRule(Player player) {
        GameRulesChangedPacket packet = new GameRulesChangedPacket();
        packet.getGameRules().add(daylightCycle(player));
        return packet;
    }

    public static void writeGameRules(Player player, List<GameRuleData<?>> rules) {
        NetworkUtils.gameRulesToNetwork(player.getLevel().getGameRules(), rules);
        rules.removeIf(rule -> rule.getName().equals(GameRules.DO_DAYLIGHT_CYCLE.getName()));
        rules.add(daylightCycle(player));
    }

    private static GameRuleData<Boolean> daylightCycle(Player player) {
        return new GameRuleData<>(GameRules.DO_DAYLIGHT_CYCLE.getName(), advancesAutomatically(player));
    }
}
