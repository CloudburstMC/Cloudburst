package org.cloudburstmc.api;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.ban.BanList;
import org.cloudburstmc.api.boss.BossBar;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.LevelBuilder;
import org.cloudburstmc.api.map.Maps;
import org.cloudburstmc.api.permission.PermissionManager;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.api.registry.*;
import org.cloudburstmc.api.scheduler.AsyncScheduler;
import org.cloudburstmc.api.scheduler.GlobalScheduler;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Access to connected players, loaded levels, registries, and server services.
 */
public interface Server {

    JsonMapper JSON_MAPPER = new JsonMapper();
    String BROADCAST_CHANNEL_ADMINISTRATIVE = "cloudburst.broadcast.admin";
    String BROADCAST_CHANNEL_USERS = "cloudburst.broadcast.user";

    /**
     * Returns the server software's name.
     */
    String getName();

    /**
     * Returns the supported Minecraft version.
     */
    String getVersion();

    /**
     * Returns the server implementation's build version.
     */
    String getImplementationVersion();

    /**
     * Returns the version of the plugin API.
     */
    String getApiVersion();

    /**
     * Requests an orderly server shutdown.
     */
    void shutdown();

    /**
     * Returns whether the server is running.
     */
    boolean isRunning();

    /**
     * Returns the plugin manager.
     */
    PluginManager getPluginManager();

    /**
     * Returns the registry of block-entity types and factories.
     */
    BlockEntityRegistry getBlockEntityRegistry();

    /**
     * Returns the registry of block types and their components.
     */
    BlockRegistry getBlockRegistry();

    /**
     * Returns the biome registry.
     */
    BiomeRegistry getBiomeRegistry();

    /**
     * Returns the registry of damage types.
     *
     * @return the damage type registry
     */
    DamageTypeRegistry getDamageTypeRegistry();

    /**
     * Returns the status-effect registry.
     */
    EffectRegistry getEffectRegistry();

    /**
     * Returns the enchantment registry.
     */
    EnchantmentRegistry getEnchantmentRegistry();

    /**
     * Returns the registry of entity types, factories, and components.
     */
    EntityRegistry getEntityRegistry();

    /**
     * Returns the game-rule registry.
     */
    GameRuleRegistry getGameRuleRegistry();

    /**
     * Returns the registry of item types and their components.
     */
    ItemRegistry getItemRegistry();

    /**
     * Returns the particle registry.
     */
    ParticleRegistry getParticleRegistry();

    /**
     * Returns the crafting recipe registry.
     */
    RecipeRegistry getRecipeRegistry();

    /**
     * Returns the resource-pack registry.
     */
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

    /**
     * Returns the current server tick count.
     */
    int getTick();

    /**
     * Returns the event manager for registering listeners and calling events.
     */
    EventManager getEventManager();

    /**
     * Returns whether flight is allowed by the server's configuration.
     */
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
     * Returns the service for creating and finding persistent maps.
     *
     * @return the map service
     */
    Maps maps();

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

    /**
     * Returns an immutable snapshot of online players keyed by UUID.
     */
    Map<UUID, ? extends Player> getOnlinePlayers();

    /**
     * Returns the default game mode for players.
     */
    GameMode getGameMode();

    /**
     * Returns the primary level.
     */
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

    /**
     * Returns the configured player limit.
     */
    int getMaxPlayers();

    /**
     * Returns whether the whitelist is enabled. Operators may join without being listed.
     */
    boolean hasWhitelist();

    /**
     * Enables or disables whitelist admission checks and persists the setting.
     * Does not disconnect players already online.
     */
    void setWhitelist(boolean enabled);

    /**
     * Returns a snapshot of explicitly whitelisted players, whether or not the whitelist is enabled.
     * Change membership through {@link OfflinePlayer#setWhitelisted(boolean)}.
     */
    Set<OfflinePlayer> getWhitelistedPlayers();

    /**
     * Reloads whitelist membership from disk. Invalid data preserves the active list.
     */
    void reloadWhitelist();

    /**
     * Returns a snapshot of server operators. Change status through
     * {@link OfflinePlayer#setOp(boolean)} or {@link Player#setOp(boolean)}.
     */
    Set<OfflinePlayer> getOperators();

    /**
     * Returns bans keyed by player UUID. Adding a ban does not automatically disconnect players.
     */
    BanList<UUID> getPlayerBanList();

    /**
     * Returns IP bans. Addresses are compared directly without DNS lookups.
     */
    BanList<InetAddress> getIpBanList();

    /**
     * Returns the configured network port.
     */
    int getPort();

    /**
     * Returns the configured bind address, or an empty string to bind to all interfaces.
     */
    String getIp();

    /**
     * Returns the server's default difficulty.
     */
    Difficulty getDifficulty();

    /**
     * @return whether Nether levels are enabled
     */
    boolean isNetherAllowed();

    /**
     * @return whether End levels are enabled
     */
    boolean isEndAllowed();

    /**
     * Finds the online player with the shortest case-insensitive name prefix match.
     * Prefer UUID-based identity when storing a reference to a player.
     *
     * @param name the full name or prefix to match
     * @return a matching player, or null when none matches
     */
    @Nullable
    Player getPlayer(String name);

    /**
     * Returns a reference to a player by UUID, whether or not they have joined before.
     * The reference's name may be unknown.
     */
    OfflinePlayer getOfflinePlayer(UUID uuid);

    /**
     * Returns a player reference using locally known name information.
     * This does not contact an authentication service. The UUID may be unknown.
     */
    OfflinePlayer getOfflinePlayer(String name);

    /**
     * Enables or disables automatic saving for the server and its loaded levels.
     */
    void setAutoSave(boolean autoSave);

    /**
     * Reloads server YAML settings and level overrides on the server thread.
     * Every file is validated before settings become active. Changes to startup-only
     * settings, including server.properties, are rejected and require a restart.
     * Plugin configurations and plugin lifecycles are not reloaded.
     *
     * @throws IllegalStateException        if called off the server thread
     * @throws IllegalArgumentException     if settings are invalid or require a restart
     * @throws java.io.UncheckedIOException if settings cannot be read
     */
    void reloadConfiguration();

    /**
     * Returns the main-thread, tick-bound global scheduler.
     */
    GlobalScheduler getGlobalScheduler();

    /**
     * Returns the off-thread, real-time async scheduler.
     */
    AsyncScheduler getAsyncScheduler();

    /**
     * Registers a player as online and updates the player list.
     * This is a server lifecycle hook, not a way for plugins to create players.
     *
     * @param who the player completing login
     */
    void addOnlinePlayer(Player who);

    /**
     * Sends the initial player list after login has completed.
     * This is a server lifecycle hook and should not be called by plugins.
     *
     * @param who the player completing login
     */
    void onPlayerCompleteLoginSequence(Player who);
}
