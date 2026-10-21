package org.cloudburstmc.server.item;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.registry.RegistryUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

class ItemPaletteTest {

    @Test
    void runtimeDefinitionsRoundTripIncludingAir() throws IOException {
        ItemPalette palette = new ItemPalette();
        List<ItemDefinition> definitions = palette.getDefinitions();

        assertEquals(definitions.size(), definitions.stream().map(ItemDefinition::getRuntimeId).distinct().count());
        for (ItemDefinition definition : definitions) {
            assertSame(definition, palette.getDefinition(definition.getRuntimeId()));
            assertTrue(palette.isRegistered(definition));
        }

        CloudItemDefinition air = requireNonNull(palette.getDefinition(Identifiers.AIR), "Missing air definition");
        assertEquals(readDumpedAirRuntimeId(), air.getRuntimeId());
        assertNotEquals(0, air.getRuntimeId());
        assertSame(air, palette.getDefinition(air.getRuntimeId()));
        assertNull(palette.getDefinition(0));
        assertSame(ItemData.AIR, ItemUtils.toNetwork(ItemStack.EMPTY));
        assertNull(palette.getDefinition(Integer.MAX_VALUE));
        assertThrows(UnsupportedOperationException.class, definitions::clear);
    }

    @Test
    void newDefinitionsDoNotMutateOtherPalettesOrReuseRuntimeIds() {
        ItemPalette first = new ItemPalette();
        ItemPalette second = new ItemPalette();
        Identifier id = Identifier.parse("test:palette_item");
        int highestRuntimeId = first.getDefinitions().stream().mapToInt(ItemDefinition::getRuntimeId).max().orElseThrow();

        first.registerItem(id);
        CloudItemDefinition definition = requireNonNull(first.getDefinition(id), "Missing registered item definition");

        assertEquals(highestRuntimeId + 1, definition.getRuntimeId());
        assertSame(definition, first.getDefinition(definition.getRuntimeId()));
        assertNull(second.getDefinition(id));
        assertNull(second.getDefinition(definition.getRuntimeId()));
        assertFalse(second.isRegistered(definition));
        assertFalse(first.isVanillaDefinition(id));

        first.registerItem(id);
        assertSame(definition, first.getDefinition(id));
    }

    @Test
    void namedAndNumericLegacyLookupsResolveTheSameVariant() {
        ItemPalette palette = new ItemPalette();
        Identifier wool = Identifier.parse("minecraft:wool");
        Identifier redWool = Identifier.parse("minecraft:red_wool");

        assertSame(palette.getDefinition(redWool), palette.getDefinition(wool, 14));
        assertEquals(redWool, palette.fromLegacy(749, 14));
        assertSame(palette.getDefinition(wool, 0), palette.getDefinition(wool, 32767));
        assertThrows(RegistryException.class, () -> palette.fromLegacy(Integer.MAX_VALUE, 0));
    }

    @Test
    void freezingPreventsFurtherRegistration() {
        ItemPalette palette = new ItemPalette();
        palette.freeze();
        palette.freeze();

        assertThrows(RegistryException.class, () -> palette.registerItem(Identifier.parse("test:closed")));
        assertThrows(RegistryException.class, palette::registerVanillaCreativeItems);
        assertNotNull(palette.getDefinition(Identifiers.AIR));
    }

    private static int readDumpedAirRuntimeId() throws IOException {
        try (InputStream input = RegistryUtils.getOrAssertResource("data/runtime_item_states.json")) {
            JsonNode entries = Bootstrap.JSON_MAPPER.readTree(input);
            for (JsonNode entry : entries) {
                if (Identifiers.AIR.toString().equals(entry.get("name").asString())) {
                    return entry.get("id").intValue();
                }
            }
        }
        throw new AssertionError("Vanilla item data has no air definition");
    }
}
