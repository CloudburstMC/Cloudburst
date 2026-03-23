package org.cloudburstmc.api;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.permission.PermissionManager;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.api.registry.GameRuleRegistry;
import org.cloudburstmc.api.scheduler.AsyncScheduler;
import org.cloudburstmc.api.scheduler.GlobalScheduler;
import tools.jackson.databind.json.JsonMapper;

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

    /**
     * Returns the server's MOTD (message of the day) as shown in the server list.
     *
     * @return the MOTD component
     */
    Component motd();

    /**
     * Sets the server's MOTD.
     *
     * @param motd the new MOTD component
     */
    void motd(Component motd);

    /**
     * Returns the server's sub-MOTD as shown in the server list.
     *
     * @return the sub-MOTD component
     */
    Component subMotd();

    /**
     * Sets the server's sub-MOTD.
     *
     * @param subMotd the new sub-MOTD component
     */
    void subMotd(Component subMotd);

    Map<UUID, ? extends Player> getOnlinePlayers();

    GameMode getGameMode();

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

    void addOp(String name);

    void removeOp(Player who);

    void removeOp(String name);

    boolean isOp(Player who);

    int getPort();

    String getIp();

    Difficulty getDifficulty();

    void addOnlinePlayer(Player who);

    void onPlayerCompleteLoginSequence(Player who);

    @Nullable
    Player getPlayer(String name);

    void setAutoSave(boolean autoSave);

    String getApiVersion();

    /**
     * Returns the main-thread, tick-bound global scheduler.
     */
    GlobalScheduler getGlobalScheduler();

    /**
     * Returns the off-thread, real-time async scheduler.
     */
    AsyncScheduler getAsyncScheduler();
}
