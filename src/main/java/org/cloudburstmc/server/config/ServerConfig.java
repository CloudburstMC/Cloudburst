package org.cloudburstmc.server.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import org.cloudburstmc.server.level.Difficulty;
import org.cloudburstmc.server.player.GameMode;

import java.util.*;

/**
 * the universal public facing facade for the server's config
 * so you dont have to care about where the config actually lies
 */
public class ServerConfig {


    public static class Properties {

        private final ServerProperties properties;

        public Properties(ServerProperties properties) {
            this.properties = properties;
        }

        public String getMotd() {
            return properties.getMotd();
        }

        public String getSubMotd() {
            return properties.getSubMotd();
        }

        public String getIp() {
            return properties.getServerIp();
        }

        public int getPort() {
            return properties.getServerPort();
        }

        public int getViewDistance() {
            return properties.getViewDistance();
        }

        public boolean getWhitelist() {
            return properties.isWhiteList();
        }

        public void setWhitelist(boolean b) {
            properties.modifyWhitelist(b);
        }

        public boolean getAchievements() {
            return properties.isAchievements();
        }

        public boolean getAnnouncePlayerAchievements() {
            return properties.isAnnouncePlayerAchievements();
        }

        public int getSpawnRadius() {
            return properties.getSpawnProtection();
        }

        public int getMaxPlayers() {
            return properties.getMaxPlayers();
        }

        public boolean getAllowFlight() {
            return properties.isAllowFlight();
        }

        public boolean getSpawnAnimals() {
            return properties.isSpawnAnimals();
        }

        public boolean getSpawnMobs() {
            return properties.isSpawnMobs();
        }

        public GameMode getGamemode() {
            return GameMode.from(properties.getGamemode());
        }

        public void setGamemode(GameMode gameMode) {
            properties.modifyGamemode(gameMode);
        }

        public boolean getForceGamemode() {
            return properties.isForceGamemode();
        }

        public boolean getHardcore() {
            return properties.isHardcore();
        }

        public boolean getPVP() {
            return properties.isPvp();
        }

        public Difficulty getDifficulty() {
            return Difficulty.values()[properties.getDifficulty()];
        }

        public void setDifficulty(Difficulty difficulty) {
            properties.modifyDifficulty(difficulty);
        }

        public String getDefaultLevel() {
            return properties.getDefaultLevel();
        }

        public boolean getAllowNether() {
            return properties.isAllowNether();
        }

        public boolean getEnableQuery() {
            return properties.isEnableQuery();
        }

        public boolean getAutoSave() {
            return properties.isAutoSave();
        }

        public boolean getForceResources() {
            return properties.isForceResources();
        }

        public boolean getGenerateStructures() {
            return properties.isGenerateStructures();
        }

        public boolean getXboxAuth() {
            return properties.isXboxAuth();
        }

    }

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

    // forwarding server.properties

    public Properties getPropertiesConfig() {
        return new Properties(properties);
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

    public ObjectNode getRootNode() {
        return cloudburstYaml.getRootNode();
    }

    // escape hatch //

    public ServerProperties getServerProperties() {
        return properties;
    }

    public CloudburstYaml getCloudburstYaml() { return cloudburstYaml; }

}
