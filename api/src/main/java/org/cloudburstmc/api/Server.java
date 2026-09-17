package org.cloudburstmc.api;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.boss.BossBar;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.LevelBuilder;
import org.cloudburstmc.api.permission.PermissionManager;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.api.registry.*;
import org.cloudburstmc.api.scheduler.AsyncScheduler;
import org.cloudburstmc.api.scheduler.GlobalScheduler;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Set;
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

    BlockEntityRegistry getBlockEntityRegistry();

    BlockRegistry getBlockRegistry();

    BiomeRegistry getBiomeRegistry();

    /**
     * Returns the registry of damage types.
     *
     * @return the damage type registry
     */
    DamageTypeRegistry getDamageTypeRegistry();

    EffectRegistry getEffectRegistry();

    EnchantmentRegistry getEnchantmentRegistry();

    EntityRegistry getEntityRegistry();

    GameRuleRegistry getGameRuleRegistry();

    ItemRegistry getItemRegistry();

    ParticleRegistry getParticleRegistry();

    RecipeRegistry getRecipeRegistry();

    ResourcePackRegistry getResourcePackRegistry();

    /**
     * Creates a boss bar with full progress.
     *
     * @param title the bar title
     * @param color the bar color
     * @param style the bar style
     * @return the new boss bar
     */
    BossBar createBossBar(Component title, BossBarColor color, BossBarStyle style);

    int getTick();

    EventManager getEventManager();

    boolean getAllowFlight();

    /**
     * Returns the registry of permission definitions.
     *
     * @return the permission manager
     */
    PermissionManager getPermissionManager();

    /**
     * Returns the command registrar and dispatcher.
     *
     * @return the command service
     */
    Commands commands();

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

    /**
     * Returns the currently loaded levels.
     *
     * @return loaded levels
     */
    Set<? extends Level> getLevels();

    /**
     * Creates a builder for loading or creating a level.
     *
     * @param id level ID and directory name
     * @return new level builder
     */
    LevelBuilder levelBuilder(String id);

    /**
     * Unloads a non-default level unless a plugin cancels the unload event.
     *
     * @param level level to unload
     * @return {@code true} when the level was unloaded
     */
    boolean unloadLevel(Level level);

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

    /**
     * @return whether Nether levels are enabled
     */
    boolean isNetherAllowed();

    /**
     * @return whether End levels are enabled
     */
    boolean isEndAllowed();

    void addOnlinePlayer(Player who);

    void onPlayerCompleteLoginSequence(Player who);

    @Nullable
    Player getPlayer(String name);

    OfflinePlayer getOfflinePlayer(UUID uuid);

    OfflinePlayer getOfflinePlayer(String name);

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
