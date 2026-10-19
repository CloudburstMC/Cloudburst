package org.cloudburstmc.server.configuration;

import org.cloudburstmc.api.configuration.Configuration;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YamlConfigurationKeyTest {

    private final YamlConfigurationLoader loader = new YamlConfigurationLoader();

    @Test
    void scalarKeysRemainLiteralWhileValuesKeepTheirTypes() throws IOException {
        Configuration configuration = loader.parse("""
                on: false
                true: 12
                123: true
                yes: 1.5
                """);

        assertEquals(false, configuration.get("on"));
        assertEquals(12, configuration.get("true"));
        assertEquals(true, configuration.get("123"));
        assertEquals(1.5, configuration.get("yes"));
        assertEquals(configuration.getValues(false), loader.parse(loader.serialize(configuration)).getValues(false));
    }

    @Test
    void nestedMappingsAndCommentsUseTheSameLiteralKeys() throws IOException {
        Configuration configuration = loader.parse("""
                populators:
                  - id: cloudburst:tree
                    # Supporting block
                    on: end_stone
                """);
        Map<?, ?> populator = configuration.getMapList("populators").getFirst();

        assertEquals("end_stone", populator.get("on"));
        assertFalse(populator.containsKey(true));
        assertEquals(List.of("Supporting block"), configuration.getComments("populators[0].on"));

        Configuration reloaded = loader.parse(loader.serialize(configuration));

        assertEquals(configuration.getValues(false), reloaded.getValues(false));
        assertEquals(configuration.getComments("populators[0].on"), reloaded.getComments("populators[0].on"));
    }

    @Test
    void duplicateLiteralKeysAndCollectionKeysAreRejected() {
        assertThrows(IOException.class, () -> loader.parse("on: 1\n'on': 2\n"));
        assertThrows(IOException.class, () -> loader.parse("123: 1\n'123': 2\n"));
        assertThrows(IOException.class, () -> loader.parse("? [a, b]\n: value\n"));
    }

    @Test
    void safeTagConstructionPreservesScalarValuesAndRejectsJavaObjects() throws IOException {
        Configuration configuration = loader.parse("""
                string: !!str 123
                integer: !!int 123
                boolean: !!bool true
                """);

        assertEquals("123", configuration.get("string"));
        assertEquals(123, configuration.get("integer"));
        assertEquals(true, configuration.get("boolean"));
        assertThrows(IOException.class, () -> loader.parse("value: !!java.lang.String test\n"));
    }

    @Test
    void mergedMappingsPreserveLiteralKeysAndExplicitOverrides() throws IOException {
        Configuration configuration = loader.parse("""
                defaults: &defaults
                  on: grass_block
                  true: 1
                populator:
                  <<: *defaults
                  on: end_stone
                """);

        assertEquals("end_stone", configuration.getString("populator.on"));
        assertEquals(1, configuration.getInt("populator.true"));
    }

    @Test
    void bundledEndHighlandsBiomePreservesItsSupportingBlockProperty() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/generator/biome/minecraft/end_highlands.yml")) {
            assertNotNull(stream);
            Configuration biome = loader.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            Map<?, ?> tree = biome.getMapList("populators").getFirst();

            assertEquals("cloudburst:tree", tree.get("id"));
            assertEquals("end_stone", tree.get("on"));
        }
    }
}
