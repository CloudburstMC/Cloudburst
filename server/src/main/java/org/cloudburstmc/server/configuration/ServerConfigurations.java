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
import java.io.UncheckedIOException;
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
    private volatile Snapshot snapshot;

    private ServerConfigurations(ServerFileLayout layout, Snapshot snapshot) {
        this.layout = layout;
        this.propertiesPath = layout.properties();
        this.snapshot = snapshot;
    }

    public static ServerConfigurations load(ServerFileLayout layout) {
        Objects.requireNonNull(layout, "layout");
        ServerConfigurations configuration = new ServerConfigurations(layout, readSnapshot(layout, true, Set.of()));
        configuration.saveProperties(configuration.snapshot.properties());
        return configuration;
    }

    /**
     * Publishes one replacement snapshot only after every file has passed validation.
     * Startup-only changes are rejected without changing active settings.
     */
    public synchronized void reload() {
        Snapshot replacement = readSnapshot(layout, false, snapshot.runtimeSettings().keySet());
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
            LevelConfiguration settings = LevelConfigurationFiles.level(layout.level(name), defaults, current.global().getSettings().getLanguage(), true);
            Map<String, LevelConfiguration> runtime = new LinkedHashMap<>(current.runtimeSettings());
            runtime.put(name, settings);
            snapshot = new Snapshot(current.properties(), current.global(), current.defaults(), current.levels(), Map.copyOf(runtime));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load settings for level " + name, e);
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

    public int getViewDistance() {
        return snapshot.properties().getViewDistance();
    }

    public boolean isAchievements() {
        return snapshot.properties().isAchievements();
    }

    public boolean isAnnouncePlayerAchievements() {
        return snapshot.properties().isAnnouncePlayerAchievements();
    }

    public int getMaxPlayers() {
        return snapshot.properties().getMaxPlayers();
    }

    public boolean isHardcore() {
        return snapshot.properties().isHardcore();
    }

    public boolean isSpawnAnimals() {
        return snapshot.properties().isSpawnAnimals();
    }

    public boolean isSpawnMobs() {
        return snapshot.properties().isSpawnMobs();
    }

    public boolean isAllowFlight() {
        return snapshot.properties().isAllowFlight();
    }

    public boolean isWhiteList() {
        return snapshot.properties().isWhiteList();
    }

    public void setWhitelist(boolean value) {
        updateProperties(properties -> properties.modifyWhitelist(value));
    }

    public String getDefaultLevel() {
        return snapshot.properties().getDefaultLevel();
    }

    public int getSpawnProtection() {
        return snapshot.properties().getSpawnProtection();
    }

    public boolean isForceGamemode() {
        return snapshot.properties().isForceGamemode();
    }

    public boolean isPVP() {
        return snapshot.properties().isPvp();
    }

    public boolean isGenerateStructures() {
        return snapshot.properties().isGenerateStructures();
    }

    public boolean isAllowNether() {
        return snapshot.properties().isAllowNether();
    }

    public boolean isAllowEnd() {
        return snapshot.properties().isAllowEnd();
    }

    public boolean isAutoSave() {
        return snapshot.properties().isAutoSave();
    }

    public boolean isForceResources() {
        return snapshot.properties().isForceResources();
    }

    public boolean isXboxAuth() {
        return snapshot.properties().isXboxAuth();
    }

    public GameMode getGamemode() {
        return GameMode.from(snapshot.properties().getGamemode());
    }

    public void setGamemode(GameMode gameMode) {
        updateProperties(properties -> properties.modifyGamemode(gameMode.getVanillaId()));
    }

    public Difficulty getDifficulty() {
        return Difficulty.fromId(snapshot.properties().getDifficulty());
    }

    public void setDifficulty(Difficulty difficulty) {
        updateProperties(properties -> properties.modifyDifficulty(difficulty));
    }

    public GlobalConfiguration.Timings getTimings() {
        return snapshot.global().getTimings();
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

    private static Snapshot readSnapshot(ServerFileLayout layout, boolean create, Set<String> additionalLevels) {
        Path propertiesPath = layout.properties();
        DedicatedServerProperties properties;

        if (!create && !Files.exists(propertiesPath)) {
            throw new ConfigurationValidationException(propertiesPath, List.of(new ConfigurationProblem("<document>", null, "must exist before reloading")));
        }

        try {
            properties = Files.exists(propertiesPath)
                    ? ServerConfigurationMappers.PROPERTIES.readValue(propertiesPath.toFile(), DedicatedServerProperties.class)
                    : new DedicatedServerProperties();
        } catch (RuntimeException e) {
            throw SettingsValidation.mappingFailure(propertiesPath, e);
        }

        SettingsValidation.validate(propertiesPath, properties);

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
                LevelConfiguration settings = LevelConfigurationFiles.level(layout.level(entry.getKey()), defaults, language, create);
                levels.put(entry.getKey(), entry.getValue());
                runtimeSettings.put(entry.getKey(), settings);
            }

            for (String name : additionalLevels) {
                if (!runtimeSettings.containsKey(name)) {
                    runtimeSettings.put(name, LevelConfigurationFiles.level(layout.level(name), defaults, language, create));
                }
            }

            return new Snapshot(properties, global, resolvedDefaults, Map.copyOf(levels), Map.copyOf(runtimeSettings));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read server settings in " + layout.directory(), e);
        }
    }

    private static void requirePrimaryLevel(Path propertiesPath, DedicatedServerProperties properties, Map<String, LevelDefinition> levels) {
        String name = properties.getDefaultLevel();
        LevelDefinition primary = levels.get(name);
        if (primary == null || primary.getDimension() != LevelDimension.OVERWORLD) {
            throw new ConfigurationValidationException(propertiesPath, List.of(new ConfigurationProblem("default-level", name, "must name a configured Overworld level")));
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
        checkRestart(problems, "timings", before.getTimings(), after.getTimings());
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
