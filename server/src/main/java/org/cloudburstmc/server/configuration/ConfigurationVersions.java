package org.cloudburstmc.server.configuration;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.configuration.ConfigurationSection;
import org.cloudburstmc.server.configuration.codec.ConfigurationNumbers;
import org.cloudburstmc.server.configuration.validation.ConfigurationProblem;
import org.cloudburstmc.server.configuration.validation.ConfigurationValidationException;

import java.nio.file.Path;
import java.util.List;

/**
 * Version gate for server-owned YAML layouts. Unsupported layouts are not migrated.
 */
@UtilityClass
public class ConfigurationVersions {
    public static final int CURRENT = 1;

    public static void requireCurrent(Path file, ConfigurationSection document) {
        Object actual = document.get("config-version", null);
        int version;

        try {
            version = ConfigurationNumbers.integer(actual, "config-version");
        } catch (IllegalArgumentException e) {
            throw invalid(file, actual);
        }

        if (version != CURRENT) {
            throw invalid(file, actual);
        }
    }

    private static ConfigurationValidationException invalid(Path file, Object actual) {
        return new ConfigurationValidationException(file, List.of(new ConfigurationProblem(
                "config-version", actual, "must be " + CURRENT + "; unsupported layouts require manual replacement")));
    }
}
