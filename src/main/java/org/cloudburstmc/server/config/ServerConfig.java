package org.cloudburstmc.server.config;

import org.cloudburstmc.server.level.Difficulty;
import org.cloudburstmc.server.player.GameMode;
import org.cloudburstmc.server.utils.ConfigSection;

import java.util.Collections;
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

    public boolean getTimingsEnabled() {
        return this.cloudburstYaml.getConfig("timings.enabled", false);
    }

    public boolean getTimingsVerbose() {
        return this.cloudburstYaml.getConfig("timings.verbose", false);
    }

    public boolean getTimingsPrivacy() {
        return this.cloudburstYaml.getConfig("timings.privacy", false);
    }

    public int getNetworkCompressionLevel() {
        return this.cloudburstYaml.getConfig("network.compression-level", 7);
    }

    public boolean getNetworkAsyncCompression() {
        return this.cloudburstYaml.getConfig("network.async-compression", true);
    }

    public boolean getLevelSettingsAutoTickRate() {
        return this.cloudburstYaml.getConfig("level-settings.auto-tick-rate", true);
    }

    public int getLevelSettingsAutoTickRateLimit() {
        return this.cloudburstYaml.getConfig("level-settings.auto-tick-rate-limit", 20);
    }

    public boolean getLevelSettingsAlwaysTickPlayers() {
        return this.cloudburstYaml.getConfig("level-settings.always-tick-players", false);
    }

    public int getLevelSettingsBaseTickRate() {
        return this.cloudburstYaml.getConfig("level-settings.base-tick-rate", 1);
    }

    public int getTicksPerAutosave() {
        return this.cloudburstYaml.getConfig("ticks-per.autosave", 6000);
    }

    public int getDebugLevel() {
        return this.cloudburstYaml.getConfig("debug.level", 1);
    }

    public int getTimingsHistoryInterval() {
        return this.cloudburstYaml.getConfig("timings.history-interval", 6000);
    }

    public int getTimingsHistoryLength() {
        return this.cloudburstYaml.getConfig("timings.history-length", 72000);
    }

    public int getChunkSendingMaxChunkRadius() {
        return this.cloudburstYaml.getConfig("chunk-sending.max-chunk-radius", 10);
    }

    public int getChunkSendingPerTick() {
        return this.cloudburstYaml.getConfig("chunk-sending.per-tick", 4);
    }

    public int getChunkSendingSpawnThreshold() {
        return this.cloudburstYaml.getConfig("chunk-sending.spawn-threshold", 56);
    }

    public int getChunkTickingTickRadius() {
        return this.cloudburstYaml.getConfig("chunk-ticking.tick-radius", 4);
    }

    public int getChunkTickingPerTick() {
        return this.cloudburstYaml.getConfig("chunk-ticking.per-tick", 40);
    }

    public boolean getChunkTickingClearTickList() {
        return this.cloudburstYaml.getConfig("chunk-ticking.clear-tick-list", true);
    }

    public boolean getSettingsQueryPlugins() {
        return this.cloudburstYaml.getConfig("settings.query-plugins", true);
    }

    public String getSettingsShutdownMessage() {
        return this.cloudburstYaml.getConfig("settings.shutdown-message", "Server closed");
    }

    public int getPlayerSkinChangeCooldown() {
        return this.cloudburstYaml.getConfig("player.skin-change-cooldown", 30);
    }

    public boolean getPlayerSavePlayerData() {
        return this.cloudburstYaml.getConfig("player.save-player-data", true);
    }

    public boolean getSettingsForceLanguage() {
        return this.cloudburstYaml.getConfig("settings.force-language", false);
    }

    public Object getSettingsAsyncWorkers() {
        return this.cloudburstYaml.getConfig("settings.async-workers", (Object) (-1));
    }

    public Object getSeedForWorld(String name) {
        return this.cloudburstYaml.getConfig("worlds." + name + ".seed", name);
    }

    public String getGeneratorForWorld(String name) {
        return this.cloudburstYaml.getConfig("worlds." + name + ".generator");
    }

    public Object getCommandAliases() {
        return this.cloudburstYaml.getConfig("aliases");
    }

    public List<String> getDebugIgnoredPackets() {
        return this.cloudburstYaml.getRawConfig().getStringList("debug.ignored-packets");
    }

    public boolean getYamlBugReport() {
        return this.cloudburstYaml.getRawConfig().getBoolean("bug-report", true);
    }

    public int getLevelSettingsChunkTimeoutAfterLoad() {
        return this.cloudburstYaml.getRawConfig().getInt("level-settings.chunk-timeout-after-load", 30);
    }

    public List<String> getTimingsIgnore() {
        return this.cloudburstYaml.getRawConfig().getStringList("timings.ignore");
    }

    public ConfigSection getRootSection() {
        return this.cloudburstYaml.getRawConfig().getRootSection();
    }

    public boolean getTimingsBypassMax() {
        return this.cloudburstYaml.getRawConfig().getBoolean("timings.bypass-max", false);
    }

    public String getLevelSettingsDefaultFormat() {
        return this.cloudburstYaml.getRawConfig().get("level-settings.default-format", "minecraft:leveldb");
    }

    public int getLevelSettingschunkTimeoutAfterLastAccess() {
        return this.cloudburstYaml.getRawConfig().getInt("level-settings.chunk-timeout-after-last-access", 120);
    }

    public String getSettingsLanguage() {
        return this.cloudburstYaml.getConfig("settings.language");
    }

    public String getOptionsForWorld(String name) {
        return this.cloudburstYaml.getConfig("worlds." + name + ".options", "");
    }

    public Map<String, Object> getWorldNames() {
        return this.cloudburstYaml.getConfig("worlds", Collections.emptyMap());
    }

    // escape hatch //

    public ServerProperties getServerProperties() {
        return properties;
    }

    public CloudburstYaml getCloudburstYaml() { return cloudburstYaml; }

}
