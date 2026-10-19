package org.cloudburstmc.server.configuration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.cloudburstmc.api.configuration.Configuration;
import org.cloudburstmc.api.configuration.ConfigurationSection;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.configuration.codec.ConfigurationResolvers;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTypedAccessTest {

    @Test
    void typedValuesRoundTripThroughYamlAndChildSections() throws Exception {
        Level level = InterfaceProxy.create(Level.class, Map.of("getId", "example"));
        UUID id = UUID.randomUUID();
        OfflinePlayer player = InterfaceProxy.create(OfflinePlayer.class, Map.of("getUniqueId", id, "getName", "Example"));
        YamlConfigurationLoader loader = new YamlConfigurationLoader(new ConfigurationResolvers(
                name -> level, key -> null, uuid -> uuid.equals(id) ? player : null, name -> player));
        Configuration configuration = loader.create();
        ConfigurationSection section = configuration.createSection("saved");
        Location location = Location.from(1, 70, 2, level);
        Color color = new Color(20, 30, 40, 50);

        section.setEncoded("location", location);
        section.setEncoded("color", color);
        section.setEncoded("player", player);
        section.setEncoded("vector", Vector3f.from(1, 1, 1));

        Configuration reloaded = loader.parse(loader.serialize(configuration));
        ConfigurationSection saved = reloaded.getConfigurationSection("saved");
        assertNotNull(saved);
        assertEquals(location, saved.getLocation("location"));
        assertEquals(color, saved.getColor("color"));
        assertSame(player, saved.getOfflinePlayer("player"));
        assertEquals(Vector3f.from(1, 1, 1), saved.getVector("vector"));
        assertTrue(saved.isColor("color"));
        assertFalse(saved.isLocation("color"));
    }

    @Test
    void explicitTypedFallbacksIgnoreDefaultsAndMalformedMatchingValuesFail() {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        Configuration configuration = loader.create();
        Configuration defaults = loader.create();
        defaults.setEncoded("color", Color.RED);
        configuration.setDefaults(defaults);

        assertEquals(Color.RED, configuration.getColor("color"));
        assertEquals(Color.BLUE, configuration.getColor("color", Color.BLUE));

        configuration.set("color", Map.of("type", "cloudburst:color", "data", Map.of("argb", "invalid")));
        assertThrows(IllegalArgumentException.class, () -> configuration.getColor("color"));

        configuration.setEncoded("color", null);
        assertEquals(Color.RED, configuration.getColor("color"));
    }

    @Test
    void stringFallbacksIgnoreDefaultsAndConvertExplicitValues() {
        Configuration configuration = new YamlConfigurationLoader().create();
        configuration.addDefault("message", "Default message");

        assertEquals("Default message", configuration.getString("message"));
        assertEquals("Fallback", configuration.getString("message", "Fallback"));
        assertNull(configuration.getString("message", null));
        assertEquals("Fallback", configuration.getString("missing", "Fallback"));
        assertNull(configuration.getString("missing", null));

        configuration.set("message", 42);

        assertEquals("42", configuration.getString("message", "Fallback"));
        assertEquals("42", configuration.getString("message", null));
    }

    @Test
    void componentsUseOnlyTheSuppliedSerializer() {
        Configuration configuration = new YamlConfigurationLoader().create();
        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
        TextComponent message = Component.text("A message");

        configuration.setComponent("message", serializer, message);

        assertEquals("A message", configuration.getString("message"));
        assertEquals(message, configuration.getComponent("message", serializer));
        assertEquals(message, configuration.getComponent("missing", serializer, message));

        Component fallback = Component.translatable("message.missing");
        assertEquals(fallback, configuration.getComponent("missing", serializer, fallback));

        configuration.setComponent("message", serializer, null);
        assertNull(configuration.getComponent("message", serializer));
    }
}
