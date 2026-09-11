package org.cloudburstmc.server.config;

import lombok.*;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.player.GameMode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The universal public facing facade for the server's config
 * so you don't have to care about where the config actually lies
 */
public class ServerConfig {
    private final ServerProperties properties;
    private final CloudburstYaml cloudburstYaml;

    public ServerConfig(ServerProperties properties, CloudburstYaml cloudburstYaml) {
        this.properties = properties;
        this.cloudburstYaml = cloudburstYaml;
    }

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

    public String getDefaultLevel() {
        return properties.getDefaultLevel();
    }

    public void setDefaultLevel(String name) {
        properties.modifyDefaultLevel(name);
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

    public Movement getMovement() {
        return cloudburstYaml.getMovement();
    }

    public Interaction getInteraction() {
        return cloudburstYaml.getInteraction();
    }

    public Level getLevel() {
        return cloudburstYaml.getLevel();
    }

    public ServerProperties getServerProperties() {
        return properties;
    }

    public CloudburstYaml getCloudburstYaml() {
        return cloudburstYaml;
    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class ChunkGeneration {

        @Builder.Default
        private int loadConcurrency = 4;

        @Builder.Default
        private int generationConcurrency = 0;

        @Builder.Default
        private int saveConcurrency = 8;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class LevelSettings {

        @Builder.Default
        private String defaultFormat = "minecraft:leveldb";

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Movement {

        // How many ticks of movement history to retain for server-side rewind
        // corrections. 40 ticks covers a 2-second round trip at 20 tps.
        @Builder.Default
        private int rewindHistorySize = 40;

        // Maximum distance (blocks) a player may claim to have moved in a
        // single input packet before the position is rejected outright.
        @Builder.Default
        private float maxPositionDelta = 50.0f;

        // Speed cap in blocks squared per tick squared. Moves exceeding this
        // rate are reverted. Gliding and active flight are exempt.
        @Builder.Default
        private float maxSpeedThreshold = 20.0f;

        // When true, the speed cap is halved.
        @Builder.Default
        private boolean strictMovement = false;

        // Minimum collision discrepancy, in blocks, that sends a correction.
        // Rejected positions within this threshold are still not applied.
        @Builder.Default
        private float positionAcceptanceThreshold = 0.5f;

        // When true, the player dismount position is strictly corrected on
        // high-latency connections.
        @Builder.Default
        private boolean strictDismount = false;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Interaction {

        // When true, entity interaction packets are validated more strictly.
        @Builder.Default
        private boolean strictEntityInteractions = false;

        // Maximum angle difference (0 to 1) between look direction and attack
        // direction. 0 allows up to 90 degrees of divergence; 1 requires an
        // exact match.
        @Builder.Default
        private float attackDirectionThreshold = 0.85f;

        // Multiplier applied to the server-side block-breaking reach distance.
        // Values above 1.0 give players extra reach tolerance.
        @Builder.Default
        private float blockBreakReachScalar = 1.5f;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class ChunkSending {

        @Builder.Default
        private int maxChunkRadius = 10;

        @Builder.Default
        private int maxLoadedChunkRadius = 10;

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
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Player {

        @Builder.Default
        private int skinChangeCooldown = 30;

        @Builder.Default
        private boolean savePlayerData = true;

        @Builder.Default
        private boolean logPlayerAddresses = true;
    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class World {

        @Builder.Default
        private Object seed = null;

        @Builder.Default
        private String generator = null;

        @Builder.Default
        private String options = null;

        @Builder.Default
        private Integer maxLiquidTicks = null;

        @Builder.Default
        private Integer waterOverLavaFlowSpeed = null;

    }

    @Data
    @Setter(AccessLevel.PRIVATE)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Level {

        // Maximum number of scheduled block ticks executed per game tick.
        // Capping this prevents TPS spikes during redstone storms while ensuring
        // delayed ticks still fire; they just carry over to the next tick.
        @Builder.Default
        private int maxBlockTicks = 65536;

        // Maximum number of scheduled liquid ticks executed per game tick.
        @Builder.Default
        private int maxLiquidTicks = 65536;

        // Tick delay for water when lava is horizontally adjacent.
        @Builder.Default
        private int waterOverLavaFlowSpeed = 5;

        // Maximum number of chained neighbor-update callbacks that may fire
        // from a single block-change event before the chain is cut and a
        // warning is logged. Prevents infinite or runaway update loops from
        // overflowing the call stack. Set to -1 to disable the cap.
        @Builder.Default
        private int maxChainedNeighborUpdates = 1_000_000;

    }
}
