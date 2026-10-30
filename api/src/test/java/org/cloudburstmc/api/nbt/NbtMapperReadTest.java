package org.cloudburstmc.api.nbt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NbtMapperReadTest {

    private final NbtMapper mapper = NbtMapper.create();

    @Test
    void roundTripPreservesValuesAndTypes() {
        AllTypes dto = new AllTypes();
        dto.b = false;
        dto.by = -7;
        dto.s = 300;
        dto.i = 70_000;
        dto.l = 1L << 40;
        dto.f = 0.25f;
        dto.d = -1.125;
        dto.str = "changed";
        dto.ba = new byte[]{9, -1, 0};
        dto.ia = new int[]{-4, 100_000};
        dto.la = new long[]{1L << 50};
        dto.names = List.of("x", "y", "z");
        dto.nested = new Nested();
        dto.nested.value = 42;
        dto.color = 3;
        dto.pos = List.of(-0.5, 8.0);
        dto.facing = Facing.SOUTH;
        dto.items = new ArrayList<>(List.of(new Nested()));
        dto.extra = NbtMap.builder().putShort("k", (short) 3).putByte("flag", (byte) 1).build();
        assertNotEquals(new AllTypes(), dto);

        NbtMap tag = mapper.write(dto);
        AllTypes read = mapper.read(tag, AllTypes.class);

        assertEquals(dto, read);
        assertEquals(tag, mapper.write(read));
        assertEquals((short) 3, assertInstanceOf(Short.class, read.extra.get("k")));
    }

    @Test
    void numericWidthsAreLenientOnRead() {
        NbtMap tag = NbtMap.builder()
                .putShort("i", (short) 7)
                .putDouble("f", 0.5)
                .putInt("s", 20)
                .putByte("b", (byte) 1)
                .build();

        Widths read = mapper.read(tag, Widths.class);

        assertEquals(7, read.i);
        assertEquals(0.5f, read.f);
        assertEquals((short) 20, read.s);
        assertTrue(read.b);
    }

    @Test
    void narrowingReadsCastLikeWrites() {
        NbtMap tag = NbtMap.builder()
                .putLong("i", (1L << 33) + 5)
                .putInt("by", 300)
                .putInt("s", 70_000)
                .putDouble("fromDouble", 2.75)
                .putFloat("l", 1.5f)
                .build();

        Narrowing read = mapper.read(tag, Narrowing.class);

        assertEquals(5, read.i);
        assertEquals((byte) 44, read.by);
        assertEquals((short) 4464, read.s);
        assertEquals(2, read.fromDouble);
        assertEquals(1L, read.l);
    }

    @Test
    void writeUsesDeclaredTypeAfterLenientRead() {
        Widths read = mapper.read(NbtMap.builder().putShort("i", (short) 7).build(), Widths.class);

        NbtMap written = mapper.write(read);

        assertEquals(7, assertInstanceOf(Integer.class, written.get("i")));
    }

    @Test
    void mismatchedKindKeepsDefaultAndContinues() {
        NbtMap tag = NbtMap.builder()
                .putInt("CustomName", 5)
                .putString("Level", "high")
                .putCompound("Speed", NbtMap.builder().putInt("a", 1).build())
                .putList("Label", NbtType.COMPOUND, NbtMap.builder().putInt("a", 1).build())
                .putInt("Count", 3)
                .build();

        Mismatched read = mapper.read(tag, Mismatched.class);

        assertEquals("x", read.customName);
        assertEquals(4, read.level);
        assertEquals(1.5f, read.speed);
        assertEquals("label", read.label);
        assertEquals(3, read.count);
    }

    @Test
    void unknownKeysAreIgnored() {
        NbtMap tag = NbtMap.builder()
                .putInt("i", 11)
                .putString("Unknown", "ignored")
                .putCompound("UnknownCompound", NbtMap.builder().putInt("a", 1).build())
                .build();

        Widths read = mapper.read(tag, Widths.class);

        assertEquals(11, read.i);
    }

    @Test
    void ownedKeysListsInheritedProperties() {
        Set<String> keys = mapper.ownedKeys(Sub.class);

        assertEquals(Set.of("BaseKey", "SubKey"), keys);
        assertEquals(Set.of("BaseKey"), mapper.ownedKeys(Base.class));
    }

    @Test
    void writeClientIncludesOnlyClientProperties() {
        NbtMap tag = mapper.writeClient(new ClientView());

        assertEquals(Set.of("Shown", "Patterns"), tag.keySet());
        assertEquals(1, assertInstanceOf(Integer.class, tag.get("Shown")));

        // A client property is sent whole, nested values included
        NbtList<?> patterns = assertInstanceOf(NbtList.class, tag.get("Patterns"));
        assertSame(NbtType.COMPOUND, patterns.getType());
        assertEquals(9, ((NbtMap) patterns.get(0)).getInt("value"));

        // The full view still holds every property
        assertEquals(Set.of("Shown", "Hidden", "Patterns"), mapper.write(new ClientView()).keySet());
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    @EqualsAndHashCode
    static class Nested {
        @JsonProperty("value")
        int value = 9;
    }

    enum Facing {
        DOWN, UP, NORTH, SOUTH;

        @JsonValue
        int id() {
            return ordinal() + 1;
        }
    }

    /**
     * The shape of the write test's data objects, with equality.
     */
    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    @EqualsAndHashCode
    static class AllTypes {
        @JsonProperty("b")
        boolean b = true;
        @JsonProperty("by")
        byte by = 2;
        @JsonProperty("s")
        short s = 3;
        @JsonProperty("i")
        int i = 4;
        @JsonProperty("l")
        long l = 5;
        @JsonProperty("f")
        float f = 6.5f;
        @JsonProperty("d")
        double d = 7.25;
        @JsonProperty("str")
        String str = "text";
        @JsonProperty("ba")
        byte[] ba = {1, 2};
        @JsonProperty("ia")
        int[] ia = {3, 4};
        @JsonProperty("la")
        long[] la = {5, 6};
        @JsonProperty("names")
        List<String> names = List.of("a", "b");
        @JsonProperty("nested")
        Nested nested = new Nested();
        @JsonProperty("color")
        @NbtTag(NbtTagType.BYTE)
        int color = 14;
        @JsonProperty("pos")
        @NbtTag(value = NbtTagType.LIST, element = NbtTagType.FLOAT)
        List<Double> pos = List.of(1.5, 2.0);
        @JsonProperty("facing")
        @NbtTag(NbtTagType.BYTE)
        Facing facing = Facing.NORTH;
        @JsonProperty("items")
        List<Nested> items = List.of();
        @JsonProperty("extra")
        NbtMap extra;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Widths {
        @JsonProperty("i")
        int i;
        @JsonProperty("f")
        float f;
        @JsonProperty("s")
        short s;
        @JsonProperty("b")
        boolean b;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Narrowing {
        @JsonProperty("i")
        int i;
        @JsonProperty("by")
        byte by;
        @JsonProperty("s")
        short s;
        @JsonProperty("fromDouble")
        int fromDouble;
        @JsonProperty("l")
        long l;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Mismatched {
        @JsonProperty("CustomName")
        String customName = "x";
        @JsonProperty("Level")
        int level = 4;
        @JsonProperty("Speed")
        float speed = 1.5f;
        @JsonProperty("Label")
        String label = "label";
        @JsonProperty("Count")
        int count;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Base {
        @JsonProperty("BaseKey")
        int base;
    }

    static class Sub extends Base {
        @JsonProperty("SubKey")
        int sub;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class ClientView {
        @JsonProperty("Shown")
        @NbtClient
        int shown = 1;
        @JsonProperty("Hidden")
        int hidden = 2;
        @JsonProperty("Patterns")
        @NbtClient
        List<Nested> patterns = List.of(new Nested());
    }
}
