package org.cloudburstmc.server.configuration;

import org.cloudburstmc.api.configuration.*;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.ServerFileLayout;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.cloudburstmc.server.configuration.validation.ConfigurationValidationException;
import org.cloudburstmc.server.plugin.CloudPluginConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLifecycleTest {

    @TempDir
    Path directory;

    private final YamlConfigurationLoader loader = new YamlConfigurationLoader();

    @Test
    void literalKeysRemainDistinctFromSectionAndCommentPaths() throws IOException {
        Configuration configuration = loader.parse("'a.b': 1\na:\n  b: 2\n'items[0]': literal\nitems:\n  - element\n");
        assertEquals(1, configuration.getInt("a\\.b"));
        assertEquals(2, configuration.getInt("a.b"));
        assertEquals("literal", configuration.getString("items\\[0\\]"));
        configuration.setComments("items\\[0\\]", List.of("literal key"));
        configuration.setComments("items[0]", List.of("list item"));

        Configuration reloaded = loader.parse(loader.serialize(configuration));
        assertEquals(configuration.getValues(false), reloaded.getValues(false));
        assertEquals(List.of("literal key"), reloaded.getComments("items\\[0\\]"));
        assertEquals(List.of("list item"), reloaded.getComments("items[0]"));
    }

    @Test
    void cyclicValuesAreRejectedWithoutChangingTheConfiguration() {
        Configuration configuration = loader.create();
        Map<String, Object> cycle = new java.util.HashMap<>();
        Map<String, Object> child = new java.util.HashMap<>();
        cycle.put("child", child);
        child.put("parent", cycle);
        assertThrows(IllegalArgumentException.class, () -> configuration.set("value", cycle));
        assertFalse(configuration.isSet("value"));
    }

    @Test
    void nestedSectionsRetainIdentityAndObserveUpdatedDefaults() {
        Configuration configuration = loader.create();
        Configuration defaults = loader.create();
        defaults.set("parent.child.value", 1);
        configuration.setDefaults(defaults);
        ConfigurationSection parent = configuration.createSection("parent");
        ConfigurationSection child = parent.createSection("child");

        assertSame(parent, child.getParent());
        assertSame(child, configuration.getConfigurationSection("parent.child"));
        assertEquals(1, child.getInt("value"));

        defaults.set("parent.child.value", 2);

        assertEquals(2, child.getInt("value"));
        assertEquals(7, child.getInt("value", 7));
        assertFalse(child.isSet("value"));
    }

    @Test
    void replacedSectionReferencesKeepTheirOriginalValues() {
        Configuration configuration = loader.create();
        ConfigurationSection original = configuration.createSection("section");
        original.set("value", 1);

        ConfigurationSection replacement = configuration.createSection("section");
        replacement.set("value", 2);

        assertEquals(1, original.getInt("value"));
        assertEquals(2, configuration.getInt("section.value"));
        assertSame(configuration, replacement.getRoot());
        assertSame(configuration, replacement.getParent());
    }

    @Test
    void defaultsRemainSeparateUntilCopied() throws IOException {
        Configuration configuration = loader.parse("section:\n  explicit: 7\n");
        Configuration defaults = loader.create();
        defaults.set("section", Map.of("explicit", 1, "inherited", 9));
        configuration.setDefaults(defaults);

        assertEquals(7, configuration.getInt("section.explicit"));
        ConfigurationSection section = configuration.getConfigurationSection("section");
        assertNotNull(section);
        assertEquals(9, section.getInt("inherited"));
        assertFalse(configuration.isSet("section.inherited"));
        assertFalse(loader.serialize(configuration).contains("inherited"));

        configuration.copyDefaults();
        assertTrue(configuration.isSet("section.inherited"));
        assertEquals(7, configuration.getInt("section.explicit"));
    }

    @Test
    void explicitFallbackIgnoresDefaultsAndEnumerationExcludesThem() {
        Configuration configuration = loader.create();
        configuration.addDefault("value", 9);
        assertEquals(9, configuration.getInt("value"));
        assertEquals(3, configuration.getInt("value", 3));
        assertTrue(configuration.getKeys(true).isEmpty());
        configuration.set("missing.child", null);
        assertTrue(configuration.getKeys(true).isEmpty());
    }

    @Test
    void booleanFallbackHandlesMissingAndIncompatibleValuesWithoutUsingDefaults() {
        Configuration configuration = loader.create();
        configuration.addDefault("enabled", true);

        assertTrue(configuration.getBoolean("enabled"));
        assertFalse(configuration.getBoolean("enabled", false));
        assertTrue(configuration.getBoolean("missing", true));
        assertFalse(configuration.getBoolean("missing", false));

        configuration.set("enabled", "true");

        assertFalse(configuration.getBoolean("enabled", false));
        assertTrue(configuration.getBoolean("enabled", true));

        configuration.set("enabled", false);
        assertFalse(configuration.getBoolean("enabled", true));

        configuration.set("enabled", true);
        assertTrue(configuration.getBoolean("enabled", false));
    }

    @Test
    void copyingDefaultsPreservesTheirComments() {
        Configuration configuration = loader.create();
        Configuration defaults = loader.create();
        defaults.set("value", 12);
        defaults.setComments("value", List.of("Useful setting"));
        configuration.setDefaults(defaults);

        configuration.copyDefaults();

        assertEquals(12, configuration.getInt("value"));
        assertEquals(List.of("Useful setting"), configuration.getComments("value"));
    }

    @Test
    void copyingDefaultsPreservesLiveNestedSectionsAndIncludesChainedDefaults() {
        Configuration configuration = loader.create();
        ConfigurationSection section = configuration.createSection("section");
        ConfigurationSection nested = section.createSection("nested");
        nested.set("explicit", 7);

        Configuration inherited = loader.create();
        inherited.set("section.nested.inherited", 1);
        inherited.setComments("section.nested.inherited", List.of("inherited comment"));
        Configuration defaults = loader.create();
        defaults.setDefaults(inherited);
        defaults.set("section.nested.explicit", 99);
        defaults.set("section.nested.direct", 2);
        configuration.setDefaults(defaults);

        configuration.copyDefaults();

        assertSame(section, configuration.getConfigurationSection("section"));
        assertSame(nested, section.getConfigurationSection("nested"));
        assertEquals(7, nested.getInt("explicit"));
        assertTrue(nested.isSet("inherited"));
        assertEquals(1, nested.getInt("inherited"));
        assertEquals(2, nested.getInt("direct"));
        assertEquals(List.of("inherited comment"), nested.getComments("inherited"));

        nested.set("written", 3);
        assertEquals(3, configuration.getInt("section.nested.written"));

        section.set("other", 4);
        assertSame(nested, section.getConfigurationSection("nested"));
    }

    @Test
    void commentOnlyDocumentsRetainTheirText() throws IOException {
        for (String document : List.of("# explanation\n", "---\n# explanation\n")) {
            Configuration configuration = loader.parse(document);
            Configuration reloaded = loader.parse(loader.serialize(configuration));
            assertEquals(List.of("explanation"), reloaded.getComments(""));
            assertTrue(reloaded.getValues(false).isEmpty());
        }
    }

    @Test
    void commentOnlyDocumentsRespectDisabledCommentParsing() throws IOException {
        ConfigurationLoader withoutComments = loader.withOptions(new YamlConfigurationOptions().parseComments(false));
        Configuration configuration = withoutComments.parse("# explanation\n");
        assertTrue(configuration.getValues(false).isEmpty());
        assertTrue(configuration.getComments("").isEmpty());
    }

    @Test
    void commentsOnListItemsAndFooterSurviveSerialization() throws IOException {
        Configuration configuration = loader.parse("items:\n  # first item\n  - one # inline\n  - two\n");
        configuration.setFooter(List.of("document footer"));
        Configuration reloaded = loader.parse(loader.serialize(configuration));

        assertEquals(List.of("first item"), reloaded.getComments("items[0]"));
        assertEquals(List.of("inline"), reloaded.getInlineComments("items[0]"));
        assertEquals(List.of("document footer"), reloaded.getFooter());
        assertEquals(List.of("one", "two"), reloaded.getStringList("items"));
    }

    @Test
    void duplicateKeysAreRejected() {
        assertThrows(IOException.class, () -> loader.parse("value: 1\nvalue: 2\n"));
    }

    @Test
    void configuredDocumentLimitsAreEnforced() {
        ConfigurationLoader limited = loader.withOptions(new YamlConfigurationOptions().codePointLimit(8));
        assertThrows(IOException.class, () -> limited.parse("value: too long"));
    }

    @Test
    void codecsRequireExplicitMatchingRegistrations() throws IOException {
        loader.registerCodec(Identifier.parse("test:text"), StringBuilder.class,
                new ConfigurationCodec<>() {
                    @Override
                    public Map<String, Object> encode(StringBuilder value) {
                        return Map.of("text", value.toString());
                    }

                    @Override
                    public StringBuilder decode(Map<String, Object> values) {
                        return new StringBuilder((String) values.get("text"));
                    }
                });

        Configuration configuration = loader.create();
        configuration.set("custom", loader.encode(new StringBuilder("saved")));
        Configuration reloaded = loader.parse(loader.serialize(configuration));

        ConfigurationSection section = reloaded.getConfigurationSection("custom");
        assertNotNull(section);
        Map<String, Object> encoded = section.getValues(false);

        assertEquals("saved", loader.decode(encoded, StringBuilder.class).toString());
        assertThrows(IllegalArgumentException.class, () -> loader.decode(encoded, String.class));
    }

    @Test
    void failedReloadPreservesCurrentConfiguration() throws IOException {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, "value: 12\n");
        CloudPluginConfiguration plugin = new CloudPluginConfiguration(loader, getClass(), directory);
        Configuration original = plugin.getConfig();
        Files.writeString(file, "value: [\n");

        assertThrows(IOException.class, plugin::reloadConfig);
        assertSame(original, plugin.getConfig());
        assertEquals(12, plugin.getConfig().getInt("value"));
    }

    @Test
    void failedSaveLeavesTheDestinationUntouched() throws IOException {
        Path destination = directory.resolve("occupied");
        Files.createDirectory(destination);
        Files.writeString(destination.resolve("sentinel"), "preserved");

        assertThrows(IOException.class, () -> loader.save(loader.create(), destination));
        assertEquals("preserved", Files.readString(destination.resolve("sentinel")));
    }

    @Test
    void rejectedSettingsDoNotReplaceThePropertiesFile() throws IOException {
        Files.createDirectories(directory.resolve("config"));
        Files.writeString(directory.resolve("config/cloudburst-global.yml"), "config-version: 1\nnetwork:\n  max-connections: 0\n");
        Path properties = directory.resolve("server.properties");
        Files.writeString(properties, "motd=original\n");

        assertThrows(ConfigurationValidationException.class, () -> ServerConfigurations.load(new ServerFileLayout(directory, directory.resolve("worlds"))));
        assertEquals("motd=original\n", Files.readString(properties));
    }
}
