package org.cloudburstmc.server.item;

import org.cloudburstmc.api.item.ItemDataComponentType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ItemDataComponentTypeTest {

    @Test
    void distinguishesAbsentComponentsFromFallbackValues() {
        ItemDataComponentType<Boolean> type = ItemDataComponentType.value(Identifier.parse("test:enabled"), Boolean.class);
        ItemStack stack = ItemStack.from(ItemTypes.STONE);

        assertFalse(stack.has(type));
        assertNull(stack.get(type));
        assertFalse(stack.getOrDefault(type, false));
    }

    @Test
    void storesImmutableCollectionSnapshot() {
        ItemDataComponentType<List<String>> type = ItemDataComponentType.list(Identifier.parse("test:values"), String.class);
        List<String> values = new ArrayList<>(List.of("first"));
        ItemStack stack = ItemStack.builder(ItemTypes.STONE).setData(type, values).build();

        values.add("second");

        assertTrue(stack.has(type));
        assertEquals(List.of("first"), stack.get(type));
        assertThrows(UnsupportedOperationException.class, () -> stack.getOrDefault(type, List.of()).add("third"));
    }

    @Test
    void canonicalizesRegisteredComponentTypes() {
        Identifier id = Identifier.parse("test:canonical");

        ItemDataComponentType<String> first = ItemDataComponentType.value(id, String.class);
        ItemDataComponentType<String> second = ItemDataComponentType.value(id, String.class);

        assertSame(first, second);
        assertThrows(IllegalArgumentException.class, () -> ItemDataComponentType.value(id, Integer.class));
    }
}
