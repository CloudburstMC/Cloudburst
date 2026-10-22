package org.cloudburstmc.server.configuration;

import org.cloudburstmc.server.ServerFileLayout;

import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.cloudburstmc.server.configuration.validation.ConfigurationValidationException;
import org.cloudburstmc.server.level.provider.migration.StagedLevelImport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ServerConfigurationReloadTest {

    @TempDir
    Path directory;

    @Test
    void levelSettingsAreCreatedOnlyInThePublishedDestination() throws Exception {
        prepare();
        ServerFileLayout layout = new ServerFileLayout(directory, directory.resolve("worlds"));
        Path source = directory.resolve("worlds/example");
        Files.delete(layout.level("example"));
        Files.writeString(source.resolve("level.dat"), "original metadata");

        ServerConfigurations config = ServerConfigurations.load(layout);
        config.initializeLevelConfiguration("example");
        config.initializeLevelConfiguration("dynamic");

        assertFalse(Files.exists(layout.level("example")));
        assertFalse(Files.exists(directory.resolve("worlds/dynamic")));
        assertEquals(40, config.getLevelConfiguration("example").getLevel().getMaxLiquidTicks());
        assertEquals(40, config.getLevelConfiguration("dynamic").getLevel().getMaxLiquidTicks());

        Path backup;
        try (StagedLevelImport staging = new StagedLevelImport(directory.resolve("worlds"), "example", source)) {
            Path converted = Files.createDirectory(staging.stagingRoot().resolve("example"));
            Files.writeString(converted.resolve("level.dat"), "converted metadata");
            backup = staging.publish();
        }

        config.createLevelConfiguration("example");
        config.reload();

        assertEquals("original metadata", Files.readString(backup.resolve("level.dat")));
        assertFalse(Files.exists(backup.resolve("cloudburst-level.yml")));
        assertTrue(Files.exists(layout.level("example")));
        assertEquals("converted metadata", Files.readString(source.resolve("level.dat")));
        assertEquals(40, config.getLevelConfiguration("example").getLevel().getMaxLiquidTicks());
        assertFalse(Files.exists(directory.resolve("worlds/dynamic")));
    }

    @Test
    void sparseOverridesInheritDefaultsWithoutBeingRewritten() throws Exception {
        prepare();
        Path overrides = directory.resolve("worlds/example/cloudburst-level.yml");
        String original = Files.readString(overrides);
        ServerConfigurations config = ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds")));
        LevelConfiguration settings = config.getLevelConfiguration("example");

        assertEquals(40, settings.getLevel().getMaxLiquidTicks());
        assertEquals(8, settings.getLevel().getWaterOverLavaFlowSpeed());
        assertEquals(3, settings.getChunkSending().getPerTick());
        assertEquals(10, settings.getChunkTicking().getPerTick());
        assertNull(config.getLevels().get("example").getSeed());
        assertEquals(original, Files.readString(overrides));
    }

    @Test
    void reloadPublishesGlobalAndLevelChangesTogether() throws Exception {
        prepare();
        ServerConfigurations config = ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds")));
        writeGlobal(30);
        Files.writeString(directory.resolve("worlds/example/cloudburst-level.yml"),
                "config-version: 1\nlevel:\n  water-over-lava-flow-speed: 9\n");

        config.reload();

        assertEquals(30, config.getMovement().getMaxSpeedThreshold());
        assertEquals(9, config.getLevelConfiguration("example").getLevel().getWaterOverLavaFlowSpeed());
        assertEquals(40, config.getLevelConfiguration("example").getLevel().getMaxLiquidTicks());
    }

    @Test
    void dynamicallyLoadedLevelsUseOverridesAndParticipateInReloads() throws Exception {
        prepare();
        ServerConfigurations config = ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds")));
        Path file = directory.resolve("worlds/dynamic/cloudburst-level.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "config-version: 1\nlevel:\n  max-liquid-ticks: 12\n");
        config.initializeLevelConfiguration("dynamic");

        assertEquals(12, config.getLevelConfiguration("dynamic").getLevel().getMaxLiquidTicks());

        Files.writeString(file, "config-version: 1\nlevel:\n  max-liquid-ticks: 18\n");
        config.reload();

        assertEquals(18, config.getLevelConfiguration("dynamic").getLevel().getMaxLiquidTicks());
    }

    @Test
    void invalidLevelSettingsPreventEveryChangeFromBecomingActive() throws Exception {
        prepare();
        ServerConfigurations config = ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds")));
        writeGlobal(30);
        Files.writeString(directory.resolve("worlds/example/cloudburst-level.yml"),
                "config-version: 1\nlevel:\n  max-liquid-ticks: 0\n");

        ConfigurationValidationException exception = assertThrows(ConfigurationValidationException.class, config::reload);

        assertEquals(directory.resolve("worlds/example/cloudburst-level.yml").toAbsolutePath(), exception.getFile());
        assertEquals(20, config.getMovement().getMaxSpeedThreshold());
        assertEquals(40, config.getLevelConfiguration("example").getLevel().getMaxLiquidTicks());
    }

    @Test
    void startupOnlyChangesAreRejectedWithoutChangingActiveSettings() throws Exception {
        prepare();
        ServerConfigurations config = ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds")));
        Files.writeString(directory.resolve("server.properties"), "default-level=example\nserver-port=20000\n");

        ConfigurationValidationException exception = assertThrows(ConfigurationValidationException.class, config::reload);

        assertEquals(directory.resolve("server.properties"), exception.getFile());
        assertEquals(19132, config.getServerPort());
        assertTrue(exception.getMessage().contains("restart"));
    }

    @Test
    void unsupportedOrMissingVersionsAreRejected() throws Exception {
        prepare();
        Path file = directory.resolve("config/cloudburst-global.yml");

        Files.writeString(file, "settings: {}\n");
        assertThrows(ConfigurationValidationException.class, () -> ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds"))));

        Files.writeString(file, "config-version: 2\n");
        assertThrows(ConfigurationValidationException.class, () -> ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds"))));

        writeGlobal(20);
        Files.writeString(directory.resolve("worlds/example/cloudburst-level.yml"), "level: {}\n");
        assertThrows(ConfigurationValidationException.class, () -> ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds"))));
    }

    @Test
    void overridesFollowTheConfiguredLevelDataDirectory() throws Exception {
        prepare();
        Path levels = directory.resolve("custom-levels");
        Path overrides = levels.resolve("example/cloudburst-level.yml");
        Files.createDirectories(overrides.getParent());
        Files.writeString(overrides, "config-version: 1\nlevel:\n  max-liquid-ticks: 17\n");

        ServerConfigurations config = ServerConfigurations.load(new ServerFileLayout(directory, levels));

        assertEquals(17, config.getLevelConfiguration("example").getLevel().getMaxLiquidTicks());
        assertThrows(IllegalStateException.class, () -> config.getLevelConfiguration("missing"));
    }

    @Test
    void obsoleteFilesAreIgnoredAndLeftUnchanged() throws Exception {
        prepare();
        String[] files = {"cloudburst.yml", "level-defaults.yml", "ops.txt", "white-list.txt"};
        for (String name : files) {
            Files.writeString(directory.resolve(name), "obsolete data\n");
        }

        for (String name : new String[]{"players", "maps", "level-settings"}) {
            Files.createDirectories(directory.resolve(name));
            Files.writeString(directory.resolve(name).resolve("untouched"), "obsolete data\n");
        }

        ServerConfigurations config = ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds")));
        config.reload();

        assertEquals(20, config.getMovement().getMaxSpeedThreshold());
        for (String name : files) {
            assertEquals("obsolete data\n", Files.readString(directory.resolve(name)));
        }

        for (String name : new String[]{"players", "maps", "level-settings"}) {
            assertEquals("obsolete data\n", Files.readString(directory.resolve(name).resolve("untouched")));
        }
    }

    @Test
    void obsoleteStorageGroupHasNoCompatibilityAlias() throws Exception {
        prepare();
        Files.writeString(directory.resolve("config/cloudburst-global.yml"), """
                config-version: 1
                level-settings:
                  default-format: minecraft:leveldb
                """);

        assertThrows(ConfigurationValidationException.class,
                () -> ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds"))));
    }

    @Test
    void levelPathsCannotEscapeOrAliasAnotherLevelDirectory() {
        ServerFileLayout layout = new ServerFileLayout(directory, directory.resolve("worlds"));

        assertThrows(IllegalArgumentException.class, () -> layout.level("../outside"));
        assertThrows(IllegalArgumentException.class, () -> layout.level("nested/../example"));
        assertThrows(IllegalArgumentException.class, () -> layout.level("."));
        assertThrows(IllegalArgumentException.class, () -> layout.level(" "));
    }

    @Test
    void bundledLevelDefaultsMatchTheModelDefaults() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/cloudburst-level-defaults.yml")) {
            assertNotNull(stream);
            String yaml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            LevelConfiguration settings = LevelConfigurationFiles.resolve(
                    directory.resolve("config/cloudburst-level-defaults.yml"), new YamlConfigurationLoader().parse(yaml));

            assertEquals(new LevelConfiguration(), settings);
        }
    }

    @Test
    void primaryLevelMustBeAConfiguredOverworldBeforeStorageIsInitialized() throws Exception {
        prepare();
        ServerFileLayout layout = new ServerFileLayout(directory, directory.resolve("worlds"));
        for (String name : new String[]{"missing", "example"}) {
            Files.writeString(layout.properties(), "default-level=" + name + "\n");
            Files.writeString(layout.global(), """
                    config-version: 1
                    levels:
                      example:
                        dimension: the_end
                    """);

            ConfigurationValidationException exception = assertThrows(ConfigurationValidationException.class,
                    () -> ServerConfigurations.load(layout));

            assertTrue(exception.getMessage().contains("default-level"));
            assertFalse(Files.exists(layout.playerData(name)));
            assertFalse(Files.exists(layout.savedData(name)));
            assertFalse(Files.exists(layout.userCache()));
        }
    }

    @Test
    void chunkThreadCountChangesRequireRestartAndPreserveActiveSettings() throws Exception {
        prepare();
        ServerConfigurations config = ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds")));
        Files.writeString(directory.resolve("config/cloudburst-global.yml"),
                "chunk-system:\n  worker-threads: 3\n  io-threads: 2\n", java.nio.file.StandardOpenOption.APPEND);

        ConfigurationValidationException failure = assertThrows(ConfigurationValidationException.class, config::reload);

        assertTrue(failure.getMessage().contains("chunk-system"));
        assertEquals(-1, config.getChunkSystem().getWorkerThreads());
        assertEquals(-1, config.getChunkSystem().getIoThreads());
    }

    private void prepare() throws Exception {
        Files.writeString(directory.resolve("server.properties"), "default-level=example\n");
        writeGlobal(20);
        Files.writeString(directory.resolve("config/cloudburst-level-defaults.yml"), """
                config-version: 1
                level:
                  max-liquid-ticks: 40
                chunk-ticking:
                  per-tick: 10
                """);
        Files.createDirectories(directory.resolve("worlds/example"));
        Files.writeString(directory.resolve("worlds/example/cloudburst-level.yml"), """
                config-version: 1
                level:
                  water-over-lava-flow-speed: 8
                chunk-sending:
                  per-tick: 3
                """);
    }

    private void writeGlobal(int speed) throws Exception {
        Files.createDirectories(directory.resolve("config"));
        Files.writeString(directory.resolve("config/cloudburst-global.yml"), """
                config-version: 1
                movement:
                  max-speed-threshold: %s
                levels:
                  example:
                    dimension: overworld
                    generator: cloudburst:standard
                    options: overworld
                """.formatted(speed));
    }
}
