package org.cloudburstmc.server.config;

import lombok.*;
import org.cloudburstmc.server.level.Difficulty;
import org.cloudburstmc.server.player.GameMode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * the universal public facing facade for the server's config
 * so you dont have to care about where the config actually lies
 */
public class ServerConfig {

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Network {

        @Builder.Default
        private int compressionLevel = 7;

        @Builder.Default
        private boolean asyncCompression = true;

        @Builder.Default
        private int batchThreshold = 256;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChunkTicking {

        @Builder.Default
        private int tickRadius = 4;

        @Builder.Default
        private int perTick = 40;

        @Builder.Default
        private boolean clearTickList = true;

        @Builder.Default
        private boolean lightUpdates = false;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChunkGeneration {

        @Builder.Default
        private int queueSize = 8;

        @Builder.Default
        private int populationQueueSize = 8;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Debug {

        @Builder.Default
        private int level = 1;

        @Builder.Default
        private List<String> ignoredPackets = Collections.emptyList();

        @Builder.Default
        private boolean bugReport = true;

        @Builder.Default
        private boolean commands = false;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LevelSettings {

        @Builder.Default
        private boolean autoTickRate = true;

        @Builder.Default
        private int autoTickRateLimit = 20;

        @Builder.Default
        private boolean alwaysTickPlayers = false;

        @Builder.Default
        private int baseTickRate = 1;

        @Builder.Default
        private int chunkTimeoutAfterLoad = 30;

        @Builder.Default
        private String defaultFormat = "minecraft:leveldb";

        @Builder.Default
        private int chunkTimeoutAfterLastAccess = 120;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChunkSending {

        @Builder.Default
        private int maxChunkRadius = 10;

        @Builder.Default
        private int perTick = 4;

        @Builder.Default
        private int spawnThreshold = 56;

        @Builder.Default
        private boolean cacheChunks = false;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Player {

        @Builder.Default
        private int skinChangeCooldown = 30;

        @Builder.Default
        private boolean savePlayerData = true;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Settings {

        @Builder.Default
        private boolean queryPlugins = true;

        @Builder.Default
        private String shutdownMessage = "Server closed";

        @Builder.Default
        private boolean forceLanguage = false;

        @Builder.Default
        private String language = "en_US";

        @Builder.Default
        private String asyncWorkers = "auto";

        @Builder.Default
        private boolean deprecatedVerbose = true;

        @Builder.Default
        private boolean upnp = false;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SpawnLimits {

        @Builder.Default
        private int monsters = 70;

        @Builder.Default
        private int animals = 15;

        @Builder.Default
        private int waterAnimals = 5;

        @Builder.Default
        private int ambient = 15;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TicksPer {

        @Builder.Default
        private int autosave = 6000;

        @Builder.Default
        private int animalSpawns = 400;

        @Builder.Default
        private int monsterSpawns = 1;

        @Builder.Default
        private int cacheCleanup = 900;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Timings {

        @Builder.Default
        private boolean enabled = false;

        @Builder.Default
        private boolean verbose = false;

        @Builder.Default
        private boolean privacy = false;

        @Builder.Default
        private int historyInterval = 6000;

        @Builder.Default
        private int historyLength = 72000;

        @Builder.Default
        private List<String> ignore = Collections.emptyList();

        @Builder.Default
        private boolean bypassMax = false;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class World {

        @Builder.Default
        private Object seed = null;

        @Builder.Default
        private String generator = null;

        @Builder.Default
        private String options = null;

    }


    private final ServerProperties properties;

    private final CloudburstYaml cloudburstYaml;

    public ServerConfig(ServerProperties properties, CloudburstYaml cloudburstYaml) {
        this.properties = properties;
        this.cloudburstYaml = cloudburstYaml;
    }

    // forwarding server.properties //

    public String getMotd() {
        return properties.getMotd();
    }

    public int getServerPort() {
        return properties.getServerPort();
    }

    public String getSubMotd() {
        return properties.getSubMotd();
    }

    public String getServerIp() {
        return properties.getServerIp();
    }

    public int getViewDistance() {
        return properties.getViewDistance();
    }

    public boolean isAchievements() {
        return properties.isAchievements();
    }

    public boolean isAnnouncePlayerAchievements() {
        return properties.isAnnouncePlayerAchievements();
    }

    public int getMaxPlayers() {
        return properties.getMaxPlayers();
    }

    public boolean isHardcore() {
        return properties.isHardcore();
    }

    public boolean isSpawnAnimals() {
        return properties.isSpawnAnimals();
    }

    public boolean isSpawnMobs() {
        return properties.isSpawnMobs();
    }

    public boolean isAllowFlight() {
        return properties.isAllowFlight();
    }

    public void setWhitelist(boolean b) {
        properties.modifyWhitelist(b);
    }

    public void setDefaultLevel(String name) {
        properties.modifyDefaultLevel(name);
    }

    public String getDefaultLevel() {
        return properties.getDefaultLevel();
    }

    public boolean isWhiteList() {
        return properties.isWhiteList();
    }

    public int getSpawnProtection() {
        return properties.getSpawnProtection();
    }

    public boolean isForceGamemode() {
        return properties.isForceGamemode();
    }

    public boolean isPVP() {
        return properties.isPvp();
    }

    public boolean isGenerateStructures() {
        return properties.isGenerateStructures();
    }

    public boolean isAllowNether() {
        return properties.isAllowNether();
    }

    public boolean isEnableQuery() {
        return properties.isEnableQuery();
    }

    public boolean isAutoSave() {
        return properties.isAutoSave();
    }

    public boolean isForceResources() {
        return properties.isForceResources();
    }

    public boolean isXboxAuth() {
        return properties.isXboxAuth();
    }

    public GameMode getGamemode() {
        return GameMode.from(properties.getGamemode());
    }

    public void setGamemode(GameMode gameMode) {
        properties.modifyGamemode(gameMode.getVanillaId());
    }

    public Difficulty getDifficulty() {
        return Difficulty.values()[properties.getDifficulty()];
    }

    public void setDifficulty(Difficulty difficulty) {
        properties.modifyDifficulty(difficulty);
    }

    // forwarding cloudburst.yml //

    public Map<String, List<String>> getCommandAliases() {
        return cloudburstYaml.getAliases();
    }

    public Timings getTimings() {
        return cloudburstYaml.getTimings();
    }

    public Settings getSettings() {
        return cloudburstYaml.getSettings();
    }

    public Network getNetwork() {
        return cloudburstYaml.getNetwork();
    }

    public LevelSettings getLevelSettings() {
        return cloudburstYaml.getLevelSettings();
    }

    public ChunkSending getChunkSending() {
        return cloudburstYaml.getChunkSending();
    }

    public ChunkTicking getChunkTicking() {
        return cloudburstYaml.getChunkTicking();
    }

    public ChunkGeneration getChunkGeneration() {
        return cloudburstYaml.getChunkGeneration();
    }

    public Player getPlayer() {
        return cloudburstYaml.getPlayer();
    }

    public Map<String, World> getWorlds() {
        return cloudburstYaml.getWorlds();
    }

    public SpawnLimits getSpawnLimits() {
        return cloudburstYaml.getSpawnLimits();
    }

    public TicksPer getTicksPer() {
        return cloudburstYaml.getTicksPer();
    }

    public Debug getDebug() {
        return cloudburstYaml.getDebug();
    }

    // escape hatch //

    public ServerProperties getServerProperties() {
        return properties;
    }

    public CloudburstYaml getCloudburstYaml() { return cloudburstYaml; }

}
