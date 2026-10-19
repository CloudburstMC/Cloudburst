package org.cloudburstmc.server.configuration;

import org.cloudburstmc.server.ServerFileLayout;

import org.cloudburstmc.server.configuration.type.LevelDimension;
import org.cloudburstmc.server.configuration.validation.ConfigurationProblem;
import org.cloudburstmc.server.configuration.validation.ConfigurationValidationException;
import org.cloudburstmc.server.configuration.validation.SettingsValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettingsValidationTest {
    @TempDir
    Path directory;

    @Test
    void reportsAllInvalidSettingsWithTheirFilePathsAndValues() {
        GlobalConfiguration settings = GlobalConfiguration.builder()
                .network(GlobalConfiguration.Network.builder().maxConnections(0).loginTimeoutSeconds(-1).build())
                .levels(Map.of("example", LevelDefinition.builder()
                        .dimension(LevelDimension.OVERWORLD).build()))
                .build();
        Path file = directory.resolve("cloudburst.yml");

        ConfigurationValidationException exception = assertThrows(ConfigurationValidationException.class,
                () -> SettingsValidation.validate(file, settings));

        assertEquals(file, exception.getFile());
        assertTrue(exception.getProblems().stream().anyMatch(problem ->
                problem.path().equals("network.max-connections") && problem.value().equals(0)));
        assertTrue(exception.getProblems().stream().anyMatch(problem ->
                problem.path().equals("network.login-timeout-seconds") && problem.value().equals(-1)));
    }

    @Test
    void requiredSectionsAndCollectionElementsProduceSettingPaths() {
        GlobalConfiguration settings = GlobalConfiguration.builder().network(null)
                .debug(GlobalConfiguration.Debug.builder().ignoredPackets(java.util.Arrays.asList("valid", null)).build())
                .build();

        ConfigurationValidationException exception = assertThrows(ConfigurationValidationException.class,
                () -> SettingsValidation.validate(directory.resolve("cloudburst.yml"), settings));
        List<String> paths = exception.getProblems().stream().map(ConfigurationProblem::path).toList();

        assertTrue(paths.contains("network"));
        assertTrue(paths.contains("debug.ignored-packets[1]"));
    }

    @Test
    void yamlSettingsRejectFractionalIntegersAndStringBooleans() throws Exception {
        Path file = directory.resolve("cloudburst.yml");

        for (String yaml : List.of("network:\n  max-connections: 1.5\n", "movement:\n  strict-movement: 'true'\n")) {
            Files.writeString(file, "config-version: 1\n" + yaml);
            ConfigurationValidationException exception = assertThrows(ConfigurationValidationException.class, () -> GlobalConfiguration.fromFile(file));

            assertEquals(file, exception.getFile());
            assertNotEquals("<document>", exception.getProblems().getFirst().path());
        }
    }

    @Test
    void invalidPropertiesNameTheirOwnFileAndSetting() throws Exception {
        Path file = directory.resolve("server.properties");
        Files.writeString(file, "server-port=70000\n");

        ConfigurationValidationException exception = assertThrows(ConfigurationValidationException.class,
                () -> ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds"))));

        assertEquals(file, exception.getFile());
        assertEquals("server-port", exception.getProblems().getFirst().path());
        assertEquals(70000, exception.getProblems().getFirst().value());
    }

    @Test
    void defaultsAndOptionalLevelOverridesRemainValid() {
        GlobalConfiguration settings = GlobalConfiguration.builder()
                .levels(Map.of("example", LevelDefinition.builder().dimension(LevelDimension.OVERWORLD).build()))
                .build();

        assertDoesNotThrow(() -> SettingsValidation.validate(directory.resolve("cloudburst.yml"), settings));
        assertDoesNotThrow(() -> SettingsValidation.validate(directory.resolve("server.properties"), new DedicatedServerProperties()));
    }
}
