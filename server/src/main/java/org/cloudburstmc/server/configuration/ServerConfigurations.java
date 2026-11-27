package org.cloudburstmc.server.configuration;

import org.cloudburstmc.api.configuration.Configuration;
import org.cloudburstmc.api.configuration.ConfigurationPath;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.server.ServerFileLayout;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.cloudburstmc.server.configuration.type.LevelDimension;
import org.cloudburstmc.server.configuration.validation.ConfigurationProblem;
import org.cloudburstmc.server.configuration.validation.ConfigurationValidationException;
import org.cloudburstmc.server.configuration.validation.SettingsValidation;
import org.cloudburstmc.server.persistence.AtomicFiles;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Owns validated server settings and their persistence.
 */
public class ServerConfigurations {

    private final ServerFileLayout layout;
    private final Path propertiesPath;
    private final Map<String, String> propertyOverrides;
    private volatile Snapshot snapshot;

    private ServerConfigurations(ServerFileLayout layout, Map<String, String> propertyOverrides, Snapshot snapshot) {
        this.layout = layout;
        this.propertiesPath = layout.properties();
        this.propertyOverrides = propertyOverrides;
        this.snapshot = snapshot;
    }

    /**
     * Loads all settings and saves server properties, including the overrides, back to disk.
     *
     * @param propertyOverrides server property values that replace the values read from disk
     */
    public static ServerConfigurations load(ServerFileLayout layout, Map<String, String> propertyOverrides) {
        Objects.requireNonNull(layout, "layout");
        Map<String, String> overrides = Map.copyOf(propertyOverrides);
        ServerConfigurations configuration = new ServerConfigurations(layout, overrides, readSnapshot(layout, overrides, true, Set.of()));
        configuration.saveProperties(configuration.snapshot.properties());
        return configuration;
    }

    public synchronized void reload() {
        Snapshot replacement = readSnapshot(layout, propertyOverrides, false, snapshot.runtimeSettings().keySet());
        requireReloadable(snapshot, replacement);
        snapshot = replacement;
    }

    public LevelConfiguration getLevelConfiguration(String name) {
        Snapshot current = snapshot;
        LevelConfiguration settings = current.runtimeSettings().get(Objects.requireNonNull(name, "name"));
        if (settings == null) {
            throw new IllegalStateException("Settings have not been initialized for level: " + name);
        }

        return settings;
    }

    public synchronized void initializeLevelConfiguration(String name) {
        Snapshot current = snapshot;
        if (current.runtimeSettings().containsKey(name)) {
            return;
        }

        try {
            Configuration defaults = new YamlConfigurationLoader().create();
            Map<?, ?> values = ServerConfigurationMappers.SETTINGS.convertValue(current.defaults(), Map.class);
            values.forEach((key, value) -> defaults.set(ConfigurationPath.key(key.toString()), value));
            LevelConfiguration settings = LevelConfigurationFiles.level(layout.level(name), defaults);
            Map<String, LevelConfiguration> runtime = new LinkedHashMap<>(current.runtimeSettings());
            runtime.put(name, settings);
            snapshot = new Snapshot(current.properties(), current.global(), current.defaults(), current.levels(), Map.copyOf(runtime));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load settings for level " + name, e);
        }
    }

    public synchronized void createLevelConfiguration(String name) {
        try {
            ConfigurationTemplates.ensure(layout.level(name), "cloudburst-level.yml", snapshot.global().getSettings().getLanguage(), true);
            initializeLevelConfiguration(name);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create settings for level " + name, e);
        }
    }

    public String getMotd() {
        return snapshot.properties().getMotd();
    }

    public void setMotd(String value) {
        updateProperties(properties -> properties.modifyMotd(value));
    }

    public String getSubMotd() {
        return snapshot.properties().getSubMotd();
    }

    public void setSubMotd(String value) {
        updateProperties(properties -> properties.modifySubMotd(value));
    }

    public int getServerPort() {
        return snapshot.properties().getServerPort();
    }

    public String getServerIp() {
        return snapshot.properties().getServerIp();
    }

    public boolean isOnlineMode() {
        return snapshot.properties().isOnlineMode();
    }

    public int getMaxPlayers() {
        return snapshot.properties().getMaxPlayers();
    }

    public void setMaxPlayers(int value) {
        updateProperties(properties -> properties.modifyMaxPlayers(value));
    }

    public int getViewDistance() {
        return snapshot.properties().getViewDistance();
    }

    public int getSimulationDistance() {
        return snapshot.properties().getSimulationDistance();
    }

    public boolean isWhitelist() {
        return snapshot.properties().isWhitelist();
    }

    public void setWhitelist(boolean value) {
        updateProperties(properties -> properties.modifyWhitelist(value));
    }

    public boolean isEnforceWhitelist() {
        return snapshot.properties().isEnforceWhitelist();
    }

    public void setEnforceWhitelist(boolean value) {
        updateProperties(properties -> properties.modifyEnforceWhitelist(value));
    }

    public String getLevelName() {
        return snapshot.properties().getLevelName();
    }

    public GameMode getGamemode() {
        return SettingNames.byName(GameMode.class, snapshot.properties().getGamemode());
    }

    public void setGamemode(GameMode gameMode) {
        updateProperties(properties -> properties.modifyGamemode(SettingNames.name(gameMode)));
    }

    public boolean isForceGamemode() {
        return snapshot.properties().isForceGamemode();
    }

    public Difficulty getDifficulty() {
        return SettingNames.byName(Difficulty.class, snapshot.properties().getDifficulty());
    }

    public void setDifficulty(Difficulty difficulty) {
        updateProperties(properties -> properties.modifyDifficulty(SettingNames.name(difficulty)));
    }

    public boolean isHardcore() {
        return snapshot.properties().isHardcore();
    }

    public int getSpawnProtection() {
        return snapshot.properties().getSpawnProtection();
    }

    public void setSpawnProtection(int value) {
        updateProperties(properties -> properties.modifySpawnProtection(value));
    }

    public int getOpPermissionLevel() {
        return snapshot.properties().getOpPermissionLevel();
    }

    public boolean isAllowFlight() {
        return snapshot.properties().isAllowFlight();
    }

    public boolean isRequireResourcePack() {
        return snapshot.properties().isRequireResourcePack();
    }

    public int getPlayerIdleTimeout() {
        return snapshot.properties().getPlayerIdleTimeout();
    }

    public void setPlayerIdleTimeout(int value) {
        updateProperties(properties -> properties.modifyPlayerIdleTimeout(value));
    }

    public int getNetworkCompressionThreshold() {
        return snapshot.properties().getNetworkCompressionThreshold();
    }

    public int getRateLimit() {
        return snapshot.properties().getRateLimit();
    }

    public boolean isLogIps() {
        return snapshot.properties().isLogIps();
    }

    public long getMaxTickTime() {
        return snapshot.properties().getMaxTickTime();
    }

    public GlobalConfiguration.Spark getSpark() {
        return snapshot.global().getSpark();
    }

    public GlobalConfiguration.Settings getSettings() {
        return snapshot.global().getSettings();
    }

    public GlobalConfiguration.Network getNetwork() {
        return snapshot.global().getNetwork();
    }

    public GlobalConfiguration.LevelStorage getLevelStorage() {
        return snapshot.global().getLevelStorage();
    }

    public GlobalConfiguration.ChunkGeneration getChunkGeneration() {
        return snapshot.global().getChunkGeneration();
    }

    public GlobalConfiguration.ChunkSystem getChunkSystem() {
        return snapshot.global().getChunkSystem();
    }

    public GlobalConfiguration.Player getPlayer() {
        return snapshot.global().getPlayer();
    }

    public Map<String, LevelDefinition> getLevels() {
        return snapshot.levels();
    }

    public GlobalConfiguration.TicksPer getTicksPer() {
        return snapshot.global().getTicksPer();
    }

    public GlobalConfiguration.Debug getDebug() {
        return snapshot.global().getDebug();
    }

    public GlobalConfiguration.Movement getMovement() {
        return snapshot.global().getMovement();
    }

    public GlobalConfiguration.Interaction getInteraction() {
        return snapshot.global().getInteraction();
    }

    public ObjectNode diagnosticSettings() {
        return snapshot.global().getRootNode();
    }

    private synchronized void updateProperties(Consumer<DedicatedServerProperties> update) {
        Snapshot current = snapshot;
        DedicatedServerProperties replacement = ServerConfigurationMappers.PROPERTIES.convertValue(current.properties(), DedicatedServerProperties.class);
        update.accept(replacement);
        SettingsValidation.validate(propertiesPath, replacement);
        saveProperties(replacement);
        snapshot = new Snapshot(replacement, current.global(), current.defaults(), current.levels(), current.runtimeSettings());
    }

    private void saveProperties(DedicatedServerProperties values) {
        try {
            AtomicFiles.write(propertiesPath, ServerConfigurationMappers.PROPERTIES.writeValueAsString(values));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to save server properties: " + propertiesPath, e);
        }
    }

    private static Snapshot readSnapshot(ServerFileLayout layout, Map<String, String> propertyOverrides, boolean create, Set<String> additionalLevels) {
        Path propertiesPath = layout.properties();
        if (!create && !Files.exists(propertiesPath)) {
            throw new ConfigurationValidationException(propertiesPath, List.of(new ConfigurationProblem("<document>", null, "must exist before reloading")));
        }

        DedicatedServerProperties properties = readProperties(propertiesPath, propertyOverrides);
        SettingsValidation.validate(propertiesPath, properties);
        requireNamedValues(propertiesPath, properties);

        try {
            Path globalFile = layout.global();
            ConfigurationTemplates.ensure(globalFile, "cloudburst-global.yml", "en_US", create);
            GlobalConfiguration global = GlobalConfiguration.fromFile(globalFile);
            requirePrimaryLevel(propertiesPath, properties, global.getLevels());

            String language = global.getSettings().getLanguage();
            Configuration defaults = LevelConfigurationFiles.defaults(layout.levelDefaults(), language, create);
            LevelConfiguration resolvedDefaults = LevelConfigurationFiles.resolve(layout.levelDefaults(), defaults);
            Map<String, LevelDefinition> levels = new LinkedHashMap<>();
            Map<String, LevelConfiguration> runtimeSettings = new LinkedHashMap<>();

            for (Map.Entry<String, LevelDefinition> entry : global.getLevels().entrySet()) {
                LevelConfiguration settings = LevelConfigurationFiles.level(layout.level(entry.getKey()), defaults);
                levels.put(entry.getKey(), entry.getValue());
                runtimeSettings.put(entry.getKey(), settings);
            }

            for (String name : additionalLevels) {
                if (!runtimeSettings.containsKey(name)) {
                    runtimeSettings.put(name, LevelConfigurationFiles.level(layout.level(name), defaults));
                }
            }

            return new Snapshot(properties, global, resolvedDefaults, Map.copyOf(levels), Map.copyOf(runtimeSettings));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read server settings in " + layout.directory(), e);
        }
    }

    private static DedicatedServerProperties readProperties(Path path, Map<String, String> overrides) {
        Properties values = new Properties();
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    values.load(reader);
                }
            }

            values.putAll(overrides);
            return values.isEmpty()
                    ? new DedicatedServerProperties()
                    : ServerConfigurationMappers.PROPERTIES.readPropertiesAs(values, DedicatedServerProperties.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read server properties: " + path, e);
        } catch (RuntimeException e) {
            throw SettingsValidation.mappingFailure(path, e);
        }
    }

    private static void requirePrimaryLevel(Path propertiesPath, DedicatedServerProperties properties, Map<String, LevelDefinition> levels) {
        String name = properties.getLevelName();
        LevelDefinition primary = levels.get(name);
        if (primary == null || primary.getDimension() != LevelDimension.OVERWORLD) {
            throw new ConfigurationValidationException(propertiesPath, List.of(new ConfigurationProblem("level-name", name, "must name a configured Overworld level")));
        }
    }

    private static void requireNamedValues(Path propertiesPath, DedicatedServerProperties properties) {
        List<ConfigurationProblem> problems = new ArrayList<>();
        if (SettingNames.byName(GameMode.class, properties.getGamemode()) == null) {
            problems.add(new ConfigurationProblem("gamemode", properties.getGamemode(), "must name a game mode"));
        }

        if (SettingNames.byName(Difficulty.class, properties.getDifficulty()) == null) {
            problems.add(new ConfigurationProblem("difficulty", properties.getDifficulty(), "must be one of peaceful, easy, normal or hard"));
        }

        if (!problems.isEmpty()) {
            throw new ConfigurationValidationException(propertiesPath, problems);
        }
    }


    private void requireReloadable(Snapshot current, Snapshot replacement) {
        List<ConfigurationProblem> problems = new ArrayList<>();
        if (!Objects.equals(current.properties(), replacement.properties())) {
            Map<?, ?> before = ServerConfigurationMappers.SETTINGS.convertValue(current.properties(), Map.class);
            Map<?, ?> after = ServerConfigurationMappers.SETTINGS.convertValue(replacement.properties(), Map.class);

            for (Map.Entry<?, ?> entry : after.entrySet()) {
                if (!Objects.equals(before.get(entry.getKey()), entry.getValue())) {
                    String path = entry.getKey().toString().replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
                    problems.add(new ConfigurationProblem(path, entry.getValue(), "requires a server restart"));
                }
            }

            throw new ConfigurationValidationException(propertiesPath, problems);
        }

        GlobalConfiguration before = current.global();
        GlobalConfiguration after = replacement.global();
        checkRestart(problems, "settings", before.getSettings(), after.getSettings());
        checkRestart(problems, "network", before.getNetwork(), after.getNetwork());
        checkRestart(problems, "level-storage", before.getLevelStorage(), after.getLevelStorage());
        checkRestart(problems, "chunk-generation", before.getChunkGeneration(), after.getChunkGeneration());
        checkRestart(problems, "chunk-system", before.getChunkSystem(), after.getChunkSystem());
        checkRestart(problems, "player", before.getPlayer(), after.getPlayer());
        checkRestart(problems, "spark", before.getSpark(), after.getSpark());
        checkRestart(problems, "ticks-per", before.getTicksPer(), after.getTicksPer());
        checkRestart(problems, "debug", before.getDebug(), after.getDebug());
        checkRestart(problems, "levels", before.getLevels(), after.getLevels());
        checkRestart(problems, "movement.rewind-history-size", before.getMovement().getRewindHistorySize(), after.getMovement().getRewindHistorySize());

        if (!problems.isEmpty()) {
            throw new ConfigurationValidationException(layout.global(), problems);
        }
    }

    private static void checkRestart(List<ConfigurationProblem> problems, String path, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            problems.add(new ConfigurationProblem(path, after, "requires a server restart"));
        }
    }

    private record Snapshot(DedicatedServerProperties properties, GlobalConfiguration global,
                            LevelConfiguration defaults, Map<String, LevelDefinition> levels,
                            Map<String, LevelConfiguration> runtimeSettings) {
    }
}
