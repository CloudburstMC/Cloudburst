package org.cloudburstmc.server.config;

import org.cloudburstmc.server.level.Difficulty;
import org.cloudburstmc.server.player.GameMode;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.ConfigSection;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * the universal public facing facade for the server's config
 * so you dont have to care about where the config actually lies
 */
public class ServerConfig {

    private final ServerProperties properties;

    private final CloudburstYaml cloudburstYaml;

    public ServerConfig(ServerProperties properties, CloudburstYaml cloudburstYaml) {
        this.properties = properties;
        this.cloudburstYaml = cloudburstYaml;
    }

    // forwarding server.properties

    public String getMotd() {
        return properties.getMotd();
    }

    public String getSubMotd() {
        return properties.getSubMotd();
    }

    public String getIp() {
        return properties.getIp();
    }

    public int getPort() {
        return properties.getPort();
    }

    public int getViewDistance() {
        return properties.getViewDistance();
    }

    public boolean getWhitelist() {
        return properties.getWhitelist();
    }

    public void setWhitelist(boolean b) {
        properties.setWhitelist(b);
    }

    public boolean getAchievements() {
        return properties.getAchievements();
    }

    public boolean getAnnouncePlayerAchievements() {
        return properties.getAnnouncePlayerAchievements();
    }

    public int getSpawnRadius() {
        return properties.getSpawnRadius();
    }

    public int getMaxPlayers() {
        return properties.getMaxPlayers();
    }

    public boolean getAllowFlight() {
        return properties.getAllowFlight();
    }

    public boolean getSpawnAnimals() {
        return properties.getSpawnAnimals();
    }

    public boolean getSpawnMobs() {
        return properties.getSpawnMobs();
    }

    public GameMode getGamemode() {
        return properties.getGamemode();
    }

    public void setGamemode(GameMode gameMode) {
        properties.setGamemode(gameMode);
    }

    public boolean getForceGamemode() {
        return properties.getForceGamemode();
    }

    public boolean getHardcore() {
        return properties.getHardcore();
    }

    public boolean getPVP() {
        return properties.getPVP();
    }

    public Difficulty getDifficulty() {
        return properties.getDifficulty();
    }

    public void setDifficulty(Difficulty difficulty) {
        properties.setDifficulty(difficulty);
    }

    public String getDefaultLevel() {
        return properties.getDefaultLevel();
    }

    public boolean getAllowNether() {
        return properties.getAllowNether();
    }

    public boolean getEnableQuery() {
        return properties.getEnableQuery();
    }

    public boolean getAutoSave() {
        return properties.getAutoSave();
    }

    public boolean getForceResources() {
        return properties.getForceResources();
    }

    public boolean getGenerateStructures() {
        return properties.getGenerateStructures();
    }

    public boolean getBugReport() {
        return properties.getBugReport();
    }

    public boolean getXboxAuth() {
        return properties.getXboxAuth();
    }

    // forwarding cloudburst.yml //

    public Object getCommandAliases() {
        return this.cloudburstYaml.getConfig("aliases");
    }

    public boolean getYamlBugReport() {
        return this.cloudburstYaml.getRawConfig().getBoolean("bug-report", true);
    }

    public ConfigSection getRootSection() {
        return this.cloudburstYaml.getRawConfig().getRootSection();
    }

    public TimingsConfig getTimingsConfig() {
        return TimingsConfig.builder()
                .enabled(this.cloudburstYaml.getConfig("timings.enabled", false))
                .verbose(this.cloudburstYaml.getConfig("timings.verbose", false))
                .privacy(this.cloudburstYaml.getConfig("timings.privacy", false))
                .historyInterval(this.cloudburstYaml.getConfig("timings.history-interval", 6000))
                .historyLength(this.cloudburstYaml.getConfig("timings.history-length", 72000))
                .ignore(this.cloudburstYaml.getRawConfig().getStringList("timings.ignore"))
                .bypassMax(this.cloudburstYaml.getRawConfig().getBoolean("timings.bypass-max", false))
                .build()
                ;
    }

    public SettingsConfig getSettingsConfig() {
        return SettingsConfig.builder()
                .queryPlugins(this.cloudburstYaml.getConfig("settings.query-plugins", true))
                .shutdownMessage(this.cloudburstYaml.getConfig("settings.shutdown-message", "Server closed"))
                .forceLanguage(this.cloudburstYaml.getConfig("settings.force-language", false))
                .asyncWorkers(this.cloudburstYaml.getConfig("settings.async-workers", (Object) (-1)))
                .language(this.cloudburstYaml.getConfig("settings.language"))
                .build()
                ;
    }

    public NetworkConfig getNetworkConfig() {
        return NetworkConfig.builder()
                .compressionLevel(this.cloudburstYaml.getConfig("network.compression-level", 7))
                .asyncCompression(this.cloudburstYaml.getConfig("network.async-compression", true))
                .build()
                ;
    }

    public LevelSettingsConfig getLevelSettingsConfig() {
        return LevelSettingsConfig.builder()
                .autoTickRate(this.cloudburstYaml.getConfig("level-settings.auto-tick-rate", true))
                .autoTickRateLimit(this.cloudburstYaml.getConfig("level-settings.auto-tick-rate-limit", 20))
                .alwaysTickPlayers(this.cloudburstYaml.getConfig("level-settings.always-tick-players", false))
                .baseTickRate(this.cloudburstYaml.getConfig("level-settings.base-tick-rate", 1))
                .chunkTimeoutAfterLoad(this.cloudburstYaml.getRawConfig().getInt("level-settings.chunk-timeout-after-load", 30))
                .chunkTimeoutAfterLastAccess(this.cloudburstYaml.getRawConfig().getInt("level-settings.chunk-timeout-after-last-access", 120))
                .build()
                ;
    }

    public ChunkSendingConfig getChunkSendingConfig() {
        return ChunkSendingConfig.builder()
                .maxChunkRadius(this.cloudburstYaml.getConfig("chunk-sending.max-chunk-radius", 10))
                .perTick(this.cloudburstYaml.getConfig("chunk-sending.per-tick", 4))
                .spawnThreshold(this.cloudburstYaml.getConfig("chunk-sending.spawn-threshold", 56))
                .build()
                ;
    }

    public ChunkTickingConfig getChunkTickingConfig() {
        return ChunkTickingConfig.builder()
                .tickRadius(this.cloudburstYaml.getConfig("chunk-ticking.tick-radius", 4))
                .perTick(this.cloudburstYaml.getConfig("chunk-ticking.per-tick", 40))
                .clearTickList(this.cloudburstYaml.getConfig("chunk-ticking.clear-tick-list", true))
                .build()
                ;
    }

    public TicksPerConfig getTicksPerConfig() {
        return TicksPerConfig.builder()
                .autosave(this.cloudburstYaml.getConfig("ticks-per.autosave", 6000))
                .build()
                ;
    }

    public DebugConfig getDebugConfig() {
        return DebugConfig.builder()
                .level(this.cloudburstYaml.getConfig("debug.level", 1))
                .ignoredPackets(this.cloudburstYaml.getRawConfig().getStringList("debug.ignored-packets"))
                .build()
                ;
    }

    public PlayerConfig getPlayerConfig() {
        return PlayerConfig.builder()
                .skinChangeCooldown(this.cloudburstYaml.getConfig("player.skin-change-cooldown", 30))
                .savePlayerData(this.cloudburstYaml.getConfig("player.save-player-data", true))
                .build()
                ;
    }

    public Map<String, WorldConfig> getWorldConfig() {
        final Map<String, WorldConfig> result = new HashMap<>();
        final Map<String, Object> worlds = this.cloudburstYaml.getConfig("worlds", Collections.emptyMap());
        for (String name : worlds.keySet()) {
            result.put(
                    name,
                    WorldConfig.builder()
                            .seed(this.cloudburstYaml.getConfig("worlds." + name + ".seed", name))
                            .generator(this.cloudburstYaml.getConfig("worlds." + name + ".generator"))
                            .options(this.cloudburstYaml.getConfig("worlds." + name + ".options", ""))
                            .build()
            );
        }
        return Collections.unmodifiableMap(result);
    }

    // escape hatch //

    public ServerProperties getServerProperties() {
        return properties;
    }

    public CloudburstYaml getCloudburstYaml() { return cloudburstYaml; }

}
