package org.cloudburstmc.api;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.permission.PermissionManager;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.api.registry.GameRuleRegistry;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public interface Server {

    JsonMapper JSON_MAPPER = new JsonMapper();
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

    Map<UUID, ? extends Player> getOnlinePlayers();

    GameMode getGamemode();

    Level getDefaultLevel();

    int getMaxPlayers();

    boolean hasWhitelist();

    void addWhitelist(Player who);

    void removeWhitelist(Player who);

    boolean isWhitelisted(Player who);

    boolean isBanned(Player who);

    boolean isIPBanned(Player who);

    void setBanned(Player who, boolean banned, boolean byIP);

    default void ban(Player who) {
        setBanned(who, true, false);
    }

    default void banIP(Player who) {
        setBanned(who, true, true);
    }

    default void unban(Player who) {
        setBanned(who, false, false);
    }

    void addOp(Player who);

    void removeOp(Player who);

    boolean isOp(Player who);

    int getPort();

    String getIp();

    Difficulty getDifficulty();

    void addOnlinePlayer(Player who);

    void onPlayerCompleteLoginSequence(Player who);

    @Nullable
    Player getPlayer(String name);

    void setAutoSave(boolean autoSave);
}
