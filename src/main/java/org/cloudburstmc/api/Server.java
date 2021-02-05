package org.cloudburstmc.api;

import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.permission.PermissionManager;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.api.registry.GameRuleRegistry;

import java.util.Map;

public interface Server {

    String BROADCAST_CHANNEL_ADMINISTRATIVE = "cloudburst.broadcast.admin";
    String BROADCAST_CHANNEL_USERS = "cloudburst.broadcast.user";

    String getName();

    String getVersion();

    String getImplementationVersion();

    void shutdown();

    boolean isRunning();

    PluginManager getPluginManager();

    GameRuleRegistry getGameRuleRegistry();

    int getTick();

    EventManager getEventManager();

    boolean getAllowFlight();

    PermissionManager getPermissionManager();

    String getMotd();

    Map<Long, Player> getOnlinePlayers();

    GameMode getGamemode();

    Level getDefaultLevel();

    int getMaxPlayers();

    boolean hasWhitelist();

    int getPort();

    String getIp();
}
