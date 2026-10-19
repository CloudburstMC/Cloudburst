package org.cloudburstmc.server.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.*;
import org.cloudburstmc.api.configuration.Configuration;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.cloudburstmc.server.configuration.validation.ConfigurationValidationException;
import org.cloudburstmc.server.configuration.validation.SettingConstraint;
import org.cloudburstmc.server.configuration.validation.SettingsValidation;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class GlobalConfiguration {

    @Builder.Default
    @SettingConstraint(min = 1, max = 1)
    private int configVersion = ConfigurationVersions.CURRENT;

    @Builder.Default
    private Timings timings = new Timings();

    @Builder.Default
    private Settings settings = new Settings();

    @Builder.Default
    private Network network = new Network();

    @Builder.Default
    private LevelStorage levelStorage = new LevelStorage();

    @Builder.Default
    private ChunkGeneration chunkGeneration = new ChunkGeneration();

    @Builder.Default
    private ChunkSystem chunkSystem = new ChunkSystem();

    @Builder.Default
    private TicksPer ticksPer = new TicksPer();

    @Builder.Default
    private Debug debug = new Debug();

    @Builder.Default
    private Player player = new Player();

    @Builder.Default
    private Movement movement = new Movement();

    @Builder.Default
    private Interaction interaction = new Interaction();

    @Builder.Default
    private Map<String, LevelDefinition> levels = new HashMap<>();

    public static GlobalConfiguration fromFile(Path file) {
        try {
            Configuration configuration = new YamlConfigurationLoader().load(file);
            ConfigurationVersions.requireCurrent(file, configuration);
            GlobalConfiguration yaml = ServerConfigurationMappers.SETTINGS.convertValue(
                    configuration.getValues(false), GlobalConfiguration.class);

            SettingsValidation.validate(file, yaml);
            yaml.freezeCollections();

            return yaml;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read server configuration: " + file, e);
        } catch (ConfigurationValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw SettingsValidation.mappingFailure(file, e);
        }
    }

    public Map<String, LevelDefinition> getLevels() {
        return Map.copyOf(levels);
    }

    public ObjectNode getRootNode() {
        return ServerConfigurationMappers.SETTINGS.valueToTree(this);
    }

    private void freezeCollections() {
        network.advertisedAddresses = List.copyOf(network.advertisedAddresses);
        debug.ignoredPackets = List.copyOf(debug.ignoredPackets);
        timings.ignore = List.copyOf(timings.ignore);
        levels = Map.copyOf(levels);
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Network {

        @Builder.Default
        private String identityFile = "nethernet/identity.pem";

        @Builder.Default
        private List<String> advertisedAddresses = List.of();

        @Builder.Default
        private String tlsCertificate = "";

        @Builder.Default
        private String tlsPrivateKey = "";

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int maxConnections = 128;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int maxConnectionsPerAddress = 8;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int maxPendingJoins = 64;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int handshakeTimeoutSeconds = 30;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int loginTimeoutSeconds = 120;

        public List<String> getAdvertisedAddresses() {
            return List.copyOf(advertisedAddresses);
        }
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class ChunkGeneration {

        @Builder.Default
        @SettingConstraint(min = 0)
        private int loadConcurrency = 4;

        @Builder.Default
        @SettingConstraint(min = 0)
        private int generationConcurrency = 0;

        @Builder.Default
        @SettingConstraint(min = 0)
        private int saveConcurrency = 8;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class ChunkSystem {

        @Builder.Default
        @SettingConstraint(max = 32767)
        private int ioThreads = -1;

        @Builder.Default
        @SettingConstraint(max = 32767)
        private int workerThreads = -1;

        public int resolveIoThreads() {
            return Math.max(1, this.ioThreads);
        }

        public int resolveWorkerThreads(int availableProcessors) {
            if (availableProcessors < 1) {
                throw new IllegalArgumentException("Available processor count must be positive");
            }

            if (this.workerThreads > 0) {
                return this.workerThreads;
            }

            int threads = availableProcessors / 2;
            if (threads <= 4) {
                return threads <= 3 ? 1 : 2;
            }

            return threads / 2;
        }
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Debug {

        @Builder.Default
        private int level = 1;

        @Builder.Default
        private List<String> ignoredPackets = Collections.emptyList();

        @Builder.Default
        private boolean uncaughtExceptionReports = true;

        @Builder.Default
        private boolean commands = false;

        public List<String> getIgnoredPackets() {
            return List.copyOf(ignoredPackets);
        }
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class LevelStorage {

        @Builder.Default
        private String defaultFormat = "minecraft:leveldb";
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Movement {

        // How many ticks of movement history to retain for server-side rewind
        // corrections. 40 ticks covers a 2-second round trip at 20 tps.
        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int rewindHistorySize = 40;

        // Maximum distance (blocks) a player may claim to have moved in a
        // single input packet before the position is rejected outright.
        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private float maxPositionDelta = 50.0f;

        // Speed cap in blocks squared per tick squared. Moves exceeding this
        // rate are reverted. Gliding and active flight are exempt.
        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private float maxSpeedThreshold = 20.0f;

        // When true, the speed cap is halved.
        @Builder.Default
        private boolean strictMovement = false;

        // Minimum collision discrepancy, in blocks, that sends a correction.
        // Rejected positions within this threshold are still not applied.
        @Builder.Default
        @SettingConstraint(min = 0)
        private float positionAcceptanceThreshold = 0.5f;

        // When true, the player dismount position is strictly corrected on
        // high-latency connections.
        @Builder.Default
        private boolean strictDismount = false;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Interaction {

        // When true, entity interaction packets are validated more strictly.
        @Builder.Default
        private boolean strictEntityInteractions = false;

        // Maximum angle difference (0 to 1) between look direction and attack
        // direction. 0 allows up to 90 degrees of divergence; 1 requires an
        // exact match.
        @Builder.Default
        @SettingConstraint(min = 0, max = 1)
        private float attackDirectionThreshold = 0.85f;

        // Multiplier applied to the server-side block-breaking reach distance.
        // Values above 1.0 give players extra reach tolerance.
        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private float blockBreakReachScalar = 1.5f;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Player {

        @Builder.Default
        @SettingConstraint(min = 0)
        private int userCacheSize = 1000;

        @Builder.Default
        private boolean saveUserCacheOnStopOnly = false;

        @Builder.Default
        @SettingConstraint(min = 0)
        private int skinChangeCooldown = 30;

        @Builder.Default
        private boolean savePlayerData = true;

        @Builder.Default
        private boolean logPlayerAddresses = true;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Settings {

        @Builder.Default
        private String shutdownMessage = "Server closed";

        @Builder.Default
        private boolean forceLanguage = false;

        @Builder.Default
        private String language = "en_US";

        @Builder.Default
        private boolean deprecatedVerbose = true;

        @Builder.Default
        private boolean upnp = false;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class TicksPer {

        @Builder.Default
        @SettingConstraint(min = 0)
        private int autosave = 6000;

        @Builder.Default
        @SettingConstraint(min = 0)
        private int animalSpawns = 400;

        @Builder.Default
        @SettingConstraint(min = 0)
        private int monsterSpawns = 1;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int cacheCleanup = 900;
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public static class Timings {

        @Builder.Default
        private boolean enabled = false;

        @Builder.Default
        private boolean verbose = false;

        @Builder.Default
        private boolean privacy = false;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int historyInterval = 6000;

        @Builder.Default
        @SettingConstraint(min = 0, minInclusive = false)
        private int historyLength = 72000;

        @Builder.Default
        private List<String> ignore = Collections.emptyList();

        @Builder.Default
        private boolean bypassMax = false;

        public List<String> getIgnore() {
            return List.copyOf(ignore);
        }
    }
}
