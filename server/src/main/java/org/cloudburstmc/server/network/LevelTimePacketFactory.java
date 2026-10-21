package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.gamerule.GameRules;
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

    public static SetTimePacket time(Level level) {
        SetTimePacket packet = new SetTimePacket();
        packet.setTime(networkTime(level.getFullTime()));
        return packet;
    }

    public static boolean advancesAutomatically(Level level) {
        return level.hasDaylightCycle() && !level.isTimePaused() && level.getTimeRate() == 1 && level.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE);
    }

    public static GameRulesChangedPacket gameRules(Level level) {
        GameRulesChangedPacket packet = new GameRulesChangedPacket();
        writeGameRules(level, packet.getGameRules());
        return packet;
    }

    public static void writeGameRules(Level level, List<GameRuleData<?>> rules) {
        NetworkUtils.gameRulesToNetwork(level.getGameRules(), rules);
        rules.removeIf(rule -> rule.getName().equals(GameRules.DO_DAYLIGHT_CYCLE.getName()));
        rules.add(new GameRuleData<>(GameRules.DO_DAYLIGHT_CYCLE.getName(), advancesAutomatically(level)));
    }
}
