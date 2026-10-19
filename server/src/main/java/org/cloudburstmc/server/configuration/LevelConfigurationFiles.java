package org.cloudburstmc.server.configuration;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.configuration.Configuration;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.cloudburstmc.server.configuration.validation.ConfigurationValidationException;
import org.cloudburstmc.server.configuration.validation.SettingsValidation;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads shared level settings and sparse overrides without rewriting overrides with defaults.
 */
@UtilityClass
public class LevelConfigurationFiles {

    public static Configuration defaults(Path file, String language, boolean create) throws IOException {
        ConfigurationTemplates.ensure(file, "cloudburst-level-defaults.yml", language, create);
        return read(file);
    }

    public static LevelConfiguration level(Path file, Configuration defaults, String language, boolean create) throws IOException {
        ConfigurationTemplates.ensure(file, "cloudburst-level.yml", language, create);
        Configuration document = read(file);
        document.setDefaults(defaults);
        document.copyDefaults();

        return resolve(file, document);
    }

    public static LevelConfiguration resolve(Path file, Configuration document) {
        try {
            LevelConfiguration settings = ServerConfigurationMappers.SETTINGS.convertValue(document.getValues(false), LevelConfiguration.class);
            SettingsValidation.validate(file, settings);
            return settings;
        } catch (ConfigurationValidationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw SettingsValidation.mappingFailure(file, e);
        }
    }

    private static Configuration read(Path file) throws IOException {
        Configuration document = new YamlConfigurationLoader().load(file);
        ConfigurationVersions.requireCurrent(file, document);
        return document;
    }
}
