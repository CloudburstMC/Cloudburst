/*
 * This file is licensed under the MIT License (MIT).
 *
 * Copyright (c) 2014 Daniel Ennis <http://aikar.co>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package co.aikar.timings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.server.command.ServerCommand;
import org.cloudburstmc.server.config.ServerConfig;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static co.aikar.timings.TimingIdentifier.DEFAULT_GROUP;

public final class Timings {
    private static final Logger log = LogManager.getLogger(Timings.class);
    private static boolean timingsEnabled = false;
    private static boolean verboseEnabled = false;
    private static boolean privacy = false;
    private static boolean bypassMax = false;
    private static Set<String> ignoredConfigSections = new HashSet<>();

    private static final int MAX_HISTORY_FRAMES = 12;
    private static int historyInterval = -1;
    private static int historyLength = -1;

    public static final FullServerTickTiming fullServerTickTimer;
    public static final Timing timingsTickTimer;
    public static final Timing pluginEventTimer;

    public static final Timing connectionTimer;
    public static final Timing schedulerTimer;
    public static final Timing schedulerAsyncTimer;
    public static final Timing schedulerSyncTimer;
    public static final Timing commandTimer;
    public static final Timing serverCommandTimer;
    public static final Timing levelSaveTimer;

    public static final Timing playerNetworkSendTimer;
    public static final Timing playerNetworkReceiveTimer;
    public static final Timing playerChunkOrderTimer;
    public static final Timing playerChunkSendTimer;
    public static final Timing playerCommandTimer;
    public static final Timing playerEntityLookingAtTimer;
    public static final Timing playerEntityAtPositionTimer;

    public static final Timing tickEntityTimer;
    public static final Timing tickBlockEntityTimer;
    public static final Timing entityMoveTimer;
    public static final Timing entityBaseTickTimer;
    public static final Timing livingEntityBaseTickTimer;

    public static final Timing generationTimer;
    public static final Timing populationTimer;
    public static final Timing generationCallbackTimer;

    public static final Timing permissibleCalculationTimer;
    public static final Timing permissionDefaultTimer;

    static {
        fullServerTickTimer = new FullServerTickTiming();
        timingsTickTimer = TimingsManager.getTiming(DEFAULT_GROUP.name, "Timings Tick", fullServerTickTimer);
        pluginEventTimer = TimingsManager.getTiming("Plugin Events");

        connectionTimer = TimingsManager.getTiming("Connection Handler");
        schedulerTimer = TimingsManager.getTiming("Scheduler");
        schedulerAsyncTimer = TimingsManager.getTiming("## Scheduler - Async Tasks");
        schedulerSyncTimer = TimingsManager.getTiming("## Scheduler - Sync Tasks");
        commandTimer = TimingsManager.getTiming("Commands");
        serverCommandTimer = TimingsManager.getTiming("Server Command");
        levelSaveTimer = TimingsManager.getTiming("Level Save");

        playerNetworkSendTimer = TimingsManager.getTiming("Player Network Send");
        playerNetworkReceiveTimer = TimingsManager.getTiming("Player Network Receive");
        playerChunkOrderTimer = TimingsManager.getTiming("Player Order Chunks");
        playerChunkSendTimer = TimingsManager.getTiming("Player Send Chunks");
        playerCommandTimer = TimingsManager.getTiming("Player Command");
        playerEntityLookingAtTimer = TimingsManager.getTiming("## Player: Entity Looking At");
        playerEntityAtPositionTimer = TimingsManager.getTiming(DEFAULT_GROUP.name, "## Player: Entity At Position", playerEntityLookingAtTimer);

        tickEntityTimer = TimingsManager.getTiming("## Entity Tick");
        tickBlockEntityTimer = TimingsManager.getTiming("## BlockEntity Tick");
        entityMoveTimer = TimingsManager.getTiming("## Entity Move");
        entityBaseTickTimer = TimingsManager.getTiming("## Entity Base Tick");
        livingEntityBaseTickTimer = TimingsManager.getTiming("## LivingEntity Base Tick");

        generationTimer = TimingsManager.getTiming("Level Generation");
        populationTimer = TimingsManager.getTiming("Level Population");
        generationCallbackTimer = TimingsManager.getTiming("Level Generation Callback");

        permissibleCalculationTimer = TimingsManager.getTiming("Permissible Calculation");
        permissionDefaultTimer = TimingsManager.getTiming("Default Permission Calculation");
    }

    public static void init(ServerConfig.Timings config) {
        setTimingsEnabled(config.isEnabled());
        setVerboseEnabled(config.isVerbose());
        setHistoryInterval(config.getHistoryInterval());
        setHistoryLength(config.getHistoryLength());

        privacy = config.isPrivacy();
        bypassMax = config.isBypassMax();
        ignoredConfigSections.addAll(config.getIgnore());

        log.debug("Timings: \n" +
                "Enabled - " + isTimingsEnabled() + "\n" +
                "Verbose - " + isVerboseEnabled() + "\n" +
                "History Interval - " + getHistoryInterval() + "\n" +
                "History Length - " + getHistoryLength());
    }

    public static boolean isTimingsEnabled() {
        return timingsEnabled;
    }

    public static void setTimingsEnabled(boolean enabled) {
        timingsEnabled = enabled;
        TimingsManager.reset();
    }

    public static boolean isVerboseEnabled() {
        return verboseEnabled;
    }

    public static void setVerboseEnabled(boolean enabled) {
        verboseEnabled = enabled;
        TimingsManager.needsRecheckEnabled = true;
    }

    public static boolean isPrivacy() {
        return privacy;
    }

    public static Set<String> getIgnoredConfigSections() {
        return ignoredConfigSections;
    }

    public static int getHistoryInterval() {
        return historyInterval;
    }

    public static void setHistoryInterval(int interval) {
        historyInterval = Math.max(20 * 60, interval);
        // Recheck history length with the new interval.
        if (historyLength != -1) {
            setHistoryLength(historyLength);
        }
    }

    public static int getHistoryLength() {
        return historyLength;
    }

    public static void setHistoryLength(int length) {
        // Cap at 12 history frames (1 hour at 5-minute frames).
        // Servers with special permission from Aikar may bypass this via the bypassMax flag.
        int maxLength = historyInterval * MAX_HISTORY_FRAMES;
        if (maxLength <= 0) {
            // historyInterval hasn't been initialised yet; accept the value as-is.
            historyLength = length;
            return;
        }

        if (bypassMax) {
            maxLength = Integer.MAX_VALUE;
        }

        if (length > maxLength) {
            log.warn("Timings Length too high. Requested " + length + ", max is " + maxLength
                    + ". To get longer history, you must increase your interval. Set Interval to "
                    + Math.ceil((float) length / MAX_HISTORY_FRAMES)
                    + " to achieve this length.");
        }

        historyLength = Math.max(Math.min(maxLength, length), historyInterval);

        Queue<TimingsHistory> oldQueue = TimingsManager.HISTORY;
        int frames = (getHistoryLength() / getHistoryInterval());
        TimingsManager.HISTORY = new TimingsManager.BoundedQueue<>(frames);
        TimingsManager.HISTORY.addAll(oldQueue);
    }

    public static void reset() {
        TimingsManager.reset();
    }

    public static Timing getCommandTiming(ServerCommand command) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "Command: " + command.getName(), commandTimer);
    }

    public static Timing getPluginEventTiming(Class<? extends Event> event, Object listener, Method method, PluginContainer plugin) {
        Timing group = TimingsManager.getTiming(plugin.getDescription().getName(), "Combined Total", pluginEventTimer);

        return TimingsManager.getTiming(plugin.getDescription().getName(), "Event: " + listener.getClass().getName() + "."
                + (method.getName())
                + " (" + event.getSimpleName() + ")", group);
    }

    public static Timing getEntityTiming(EntityType<?> type) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "## Entity Tick: " + type.getId(), tickEntityTimer);
    }

    public static Timing getBlockEntityTiming(BlockEntity blockEntity) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "## BlockEntity Tick: " + blockEntity.getClass().getSimpleName(), tickBlockEntityTimer);
    }

    public static Timing getReceiveDataPacketTiming(BedrockPacket pk) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "## Receive Packet: " + pk.getClass().getSimpleName(), playerNetworkReceiveTimer);
    }

    public static Timing getSendDataPacketTiming(BedrockPacket pk) {
        return TimingsManager.getTiming(DEFAULT_GROUP.name, "## Send Packet: " + pk.getClass().getSimpleName(), playerNetworkSendTimer);
    }

    public static void stopServer() {
        setTimingsEnabled(false);
        TimingsManager.recheckEnabled();
    }
}
