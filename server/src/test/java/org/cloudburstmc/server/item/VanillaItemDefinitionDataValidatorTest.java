package org.cloudburstmc.server.item;

import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VanillaItemDefinitionDataValidatorTest {

    private static final Identifier ITEM_ID = Identifier.parse("minecraft:test_item");

    @Test
    void acceptsEmptyDefinitionData() {
        assertDoesNotThrow(() -> VanillaItemDefinitionDataValidator.validate(ITEM_ID, NbtMap.EMPTY));
    }

    @Test
    void acceptsBundledVanillaDefinitions() throws IOException {
        try (InputStream input = requireNonNull(VanillaItemDefinitionDataValidatorTest.class.getResourceAsStream("/data/item_components.nbt"));
             NBTInputStream nbt = NbtUtils.createGZIPReader(input)) {
            NbtMap definitions = (NbtMap) nbt.readTag();
            for (String itemId : definitions.keySet()) {
                VanillaItemDefinitionDataValidator.validate(Identifier.parse(itemId), definitions.getCompound(itemId));
            }
        }
    }

    @Test
    void acceptsSchemaAndNetworkOnlyComponents() {
        NbtMap components = NbtMap.builder()
                .putCompound("minecraft:durability", NbtMap.EMPTY)
                .putCompound("item_properties", NbtMap.EMPTY)
                .build();
        NbtMap componentData = NbtMap.builder()
                .putCompound("components", components)
                .build();

        assertDoesNotThrow(() -> VanillaItemDefinitionDataValidator.validate(ITEM_ID, componentData));
    }

    @Test
    void rejectsUnknownComponents() {
        NbtMap components = NbtMap.builder()
                .putCompound("minecraft:unknown_component", NbtMap.EMPTY)
                .build();
        NbtMap componentData = NbtMap.builder()
                .putCompound("components", components)
                .build();

        assertThrows(RegistryException.class, () -> VanillaItemDefinitionDataValidator.validate(ITEM_ID, componentData));
    }

    @Test
    void rejectsInvalidDefinitionEnvelope() {
        NbtMap componentData = NbtMap.builder()
                .putCompound("minecraft:durability", NbtMap.EMPTY)
                .build();

        assertThrows(RegistryException.class, () -> VanillaItemDefinitionDataValidator.validate(ITEM_ID, componentData));
    }
}
