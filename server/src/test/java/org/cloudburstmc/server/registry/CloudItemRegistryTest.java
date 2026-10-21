package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.item.serializer.DefaultItemSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CloudItemRegistryTest {

    private static final CloudItemRegistry REGISTRY = CloudItemRegistry.get();

    @Test
    void enumeratesCanonicalTypesWithoutAliasesOrDuplicates() {
        List<ItemType> types = REGISTRY.values();

        assertEquals(List.copyOf(ItemTypes.values()), types);
        assertEquals(types.size(), types.stream().map(ItemType::getId).distinct().count());
        assertThrows(UnsupportedOperationException.class, types::clear);

        for (ItemType type : types) {
            assertSame(type, REGISTRY.getOrThrow(type.getId()));
            assertEquals(type.getId(), REGISTRY.getId(type));
            assertNotNull(REGISTRY.getComponents(type));
        }
    }

    @Test
    void rejectsUnregisteredObjectsEvenIfTheirIdentifierIsKnown() {
        ItemType unregistered = ItemType.of(ItemTypes.APPLE.getId());

        assertThrows(RegistryException.class, () -> REGISTRY.getComponents(unregistered));
        assertThrows(RegistryException.class, () -> REGISTRY.getSerializer(unregistered));
        assertThrows(IllegalArgumentException.class, () -> REGISTRY.getId(unregistered));
        assertTrue(REGISTRY.get(Identifier.parse("test:unregistered_item")).isEmpty());
    }

    @Test
    void retainsDefaultSerializationAndImmutableComponentOrder() {
        assertSame(DefaultItemSerializer.INSTANCE, REGISTRY.getSerializer(ItemTypes.APPLE));
        assertNotNull(REGISTRY.getDataComponentSerializer(ItemDataComponents.DAMAGE));
        assertTrue(REGISTRY.getSerializedDataComponents().contains(ItemDataComponents.DAMAGE));
        assertThrows(UnsupportedOperationException.class, () -> REGISTRY.getSerializedDataComponents().clear());
    }
}
