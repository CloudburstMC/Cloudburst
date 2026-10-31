package org.cloudburstmc.api.nbt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NbtMapperReadIntoTest {

    private final NbtMapper mapper = NbtMapper.create(new TestContainerModule());

    @Test
    void readIntoKeepsValuesForAbsentKeys() {
        Simple dto = new Simple();

        Simple read = mapper.readInto(NbtMap.builder().putInt("a", 7).build(), dto);

        assertSame(dto, read);
        assertEquals(7, dto.a);
        assertEquals("x", dto.b);
    }

    @Test
    void readIntoKeepsChangedValuesForAbsentKeys() {
        Simple dto = new Simple();
        dto.b = "changed";

        mapper.readInto(NbtMap.builder().putInt("a", 7).build(), dto);

        assertEquals(7, dto.a);
        assertEquals("changed", dto.b);
    }

    @Test
    void readIntoFillsMutableValueInPlace() {
        Chest dto = new Chest();
        ItemContainer container = dto.items;
        container.setItem(0, stone(9));

        mapper.readInto(chestTag(), dto);

        assertSame(container, dto.items);
        assertTrue(dto.items.getItem(0).isEmpty());
        assertEquals(5, dto.items.getItem(1).getCount());
        assertEquals(ItemTypes.STONE, dto.items.getItem(1).getType());
        assertEquals(2, dto.items.getItem(2).getCount());
    }

    @Test
    void readFillsTheContainerTheConstructorMade() {
        Chest read = mapper.read(chestTag(), Chest.class);

        // A container the deserializer made itself would have its own size
        TestContainer container = (TestContainer) read.items;
        assertFalse(container.fromDeserializer);
        assertEquals(Chest.SIZE, container.size());
        assertEquals(5, container.getItem(1).getCount());
    }

    @Test
    void nullCurrentValueIsReplaced() {
        Chest dto = new Chest();
        dto.items = null;

        mapper.readInto(chestTag(), dto);

        TestContainer container = (TestContainer) dto.items;
        assertTrue(container.fromDeserializer);
        assertEquals(5, container.getItem(1).getCount());
    }

    @Test
    void readIntoReplacesListsAndNestedObjects() {
        Holder dto = new Holder();
        dto.names = new ArrayList<>(List.of("a"));
        dto.nested.value = 2;
        dto.nested.other = 3;
        List<String> names = dto.names;
        Nested nested = dto.nested;
        NbtMap tag = NbtMap.builder()
                .putList("names", NbtType.STRING, List.of("b"))
                .putCompound("nested", NbtMap.builder().putInt("value", 4).build())
                .build();

        mapper.readInto(tag, dto);

        // A present list or nested object replaces the current value; it is not merged
        assertEquals(List.of("b"), dto.names);
        assertEquals(List.of("a"), names);
        assertEquals(4, dto.nested.value);
        assertEquals(1, dto.nested.other);
        assertEquals(2, nested.value);
    }

    @Test
    void readIntoMismatchKeepsCurrent() {
        Simple dto = new Simple();

        mapper.readInto(NbtMap.builder().putString("a", "str").build(), dto);

        assertEquals(5, dto.a);
        assertEquals("x", dto.b);
    }

    @Test
    void readIntoMismatchKeepsChangedValue() {
        Simple dto = new Simple();
        dto.a = 8;

        mapper.readInto(NbtMap.builder().putString("a", "str").putString("b", "y").build(), dto);

        assertEquals(8, dto.a);
        assertEquals("y", dto.b);
    }

    @Test
    void readIntoMismatchKeepsMutableValue() {
        Chest dto = new Chest();
        ItemContainer container = dto.items;
        container.setItem(0, stone(9));

        mapper.readInto(NbtMap.builder().putString("Items", "oops").putInt("Lock", 3).build(), dto);

        assertSame(container, dto.items);
        assertEquals(9, dto.items.getItem(0).getCount());
        assertEquals(3, dto.lock);
    }

    @Test
    void readIntoRethrowsFailureOfTheValue() {
        Chest dto = new Chest();
        NbtMap tag = NbtMap.builder()
                .putList("Items", NbtType.COMPOUND, List.of(slot(Chest.SIZE + 4, 1)))
                .build();

        // The container rejects the slot: that is not a shape mismatch, so the read fails
        JacksonException thrown = assertThrows(JacksonException.class, () -> mapper.readInto(tag, dto));
        assertTrue(hasCause(thrown, ArrayIndexOutOfBoundsException.class));
    }

    private static NbtMap chestTag() {
        return NbtMap.builder()
                .putList("Items", NbtType.COMPOUND, List.of(slot(1, 5), slot(2, 2)))
                .build();
    }

    private static NbtMap slot(int slot, int count) {
        return NbtMap.builder().putByte("Slot", (byte) slot).putByte("Count", (byte) count).build();
    }

    private static ItemStack stone(int count) {
        return ItemStack.builder(ItemTypes.STONE).amount(count).build();
    }

    private static boolean hasCause(Throwable thrown, Class<? extends Throwable> type) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Simple {
        @JsonProperty("a")
        int a = 5;
        @JsonProperty("b")
        String b = "x";
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Chest {
        static final int SIZE = 3;

        @JsonProperty("Items")
        ItemContainer items = new TestContainer(SIZE, false);
        @JsonProperty("Lock")
        int lock;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Holder {
        @JsonProperty("names")
        List<String> names = List.of();
        @JsonProperty("nested")
        Nested nested = new Nested();
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Nested {
        @JsonProperty("value")
        int value;
        @JsonProperty("other")
        int other = 1;
    }

    /**
     * A plain container; the server registers its own implementation.
     */
    static final class TestContainer implements ItemContainer {
        final ItemStack[] slots;
        final boolean fromDeserializer;

        TestContainer(int size, boolean fromDeserializer) {
            this.slots = new ItemStack[size];
            this.fromDeserializer = fromDeserializer;
            Arrays.fill(slots, ItemStack.EMPTY);
        }

        @Override
        public int size() {
            return slots.length;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slots[slot];
        }

        @Override
        public void setItem(int slot, ItemStack itemStack) {
            slots[slot] = itemStack;
        }
    }

    static final class TestContainerModule extends SimpleModule {
        TestContainerModule() {
            super("test-container");
            addDeserializer(ItemContainer.class, new TestContainerDeserializer());
        }
    }

    /**
     * Reads a list of {@code {Slot, Count}} stone stacks, filling an existing container
     * when there is one.
     */
    static final class TestContainerDeserializer extends StdDeserializer<ItemContainer> {
        TestContainerDeserializer() {
            super(ItemContainer.class);
        }

        @Override
        public ItemContainer deserialize(JsonParser p, DeserializationContext ctxt) {
            return deserialize(p, ctxt, new TestContainer(27, true));
        }

        @Override
        public ItemContainer deserialize(JsonParser p, DeserializationContext ctxt, ItemContainer into) {
            if (!(p instanceof NbtParser parser) || !(parser.currentTag() instanceof NbtList<?> list)) {
                return (ItemContainer) ctxt.handleUnexpectedToken(ItemContainer.class, p);
            }
            p.skipChildren();
            for (int i = 0; i < into.size(); i++) {
                into.setItem(i, ItemStack.EMPTY);
            }
            for (Object entry : list) {
                NbtMap item = (NbtMap) entry;
                into.setItem(item.getByte("Slot"), stone(item.getByte("Count")));
            }
            return into;
        }

        @Override
        public Boolean supportsUpdate(DeserializationConfig config) {
            return Boolean.TRUE;
        }
    }
}
