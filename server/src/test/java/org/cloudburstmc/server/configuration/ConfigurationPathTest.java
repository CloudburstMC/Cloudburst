package org.cloudburstmc.server.configuration;

import org.cloudburstmc.api.configuration.Configuration;
import org.cloudburstmc.api.configuration.ConfigurationPath;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationPathTest {

    @Test
    void literalKeysRoundTripThroughValuesAndComments() throws IOException {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        Configuration configuration = loader.create();
        String path = ConfigurationPath.of("database.host", "items[0]", "a\\b");

        configuration.set(path, 42);
        configuration.setComments(path, List.of("A literal key"));

        Configuration reloaded = loader.parse(loader.serialize(configuration));

        assertEquals(42, reloaded.getInt(path));
        assertEquals(List.of("A literal key"), reloaded.getComments(path));
        assertFalse(reloaded.contains("database.host"));
    }

    @Test
    void emptyPathAddressesTheSectionButEmptyKeysAreRejected() {
        assertEquals("", ConfigurationPath.of());
        assertEquals("a\\.b", ConfigurationPath.key("a.b"));
        assertThrows(IllegalArgumentException.class, () -> ConfigurationPath.of("a", ""));
        assertThrows(NullPointerException.class, () -> ConfigurationPath.of("a", null));
    }
}
