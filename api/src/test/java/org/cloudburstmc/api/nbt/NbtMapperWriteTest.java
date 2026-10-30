package org.cloudburstmc.api.nbt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class NbtMapperWriteTest {

    private final NbtMapper mapper = NbtMapper.create();

    @Test
    void automaticTypesFollowJavaTypes() {
        NbtMap tag = mapper.write(new AllTypes());

        assertEquals((byte) 1, assertInstanceOf(Byte.class, tag.get("b")));
        assertEquals((byte) 2, assertInstanceOf(Byte.class, tag.get("by")));
        assertEquals((short) 3, assertInstanceOf(Short.class, tag.get("s")));
        assertEquals(4, assertInstanceOf(Integer.class, tag.get("i")));
        assertEquals(5L, assertInstanceOf(Long.class, tag.get("l")));
        assertEquals(6.5f, assertInstanceOf(Float.class, tag.get("f")));
        assertEquals(7.25, assertInstanceOf(Double.class, tag.get("d")));
        assertEquals("text", assertInstanceOf(String.class, tag.get("str")));
        assertArrayEquals(new byte[]{1, 2}, assertInstanceOf(byte[].class, tag.get("ba")));
        assertArrayEquals(new int[]{3, 4}, assertInstanceOf(int[].class, tag.get("ia")));
        assertArrayEquals(new long[]{5, 6}, assertInstanceOf(long[].class, tag.get("la")));

        NbtList<?> names = assertInstanceOf(NbtList.class, tag.get("names"));
        assertSame(NbtType.STRING, names.getType());
        assertEquals(List.of("a", "b"), names);

        NbtMap nested = assertInstanceOf(NbtMap.class, tag.get("nested"));
        assertEquals(9, assertInstanceOf(Integer.class, nested.get("value")));
    }

    @Test
    void annotationOverridesType() {
        NbtMap tag = mapper.write(new Overrides());

        assertEquals((byte) 14, assertInstanceOf(Byte.class, tag.get("color")));

        NbtList<?> pos = assertInstanceOf(NbtList.class, tag.get("pos"));
        assertSame(NbtType.FLOAT, pos.getType());
        assertEquals(List.of(1.5f, 2.0f), pos);

        assertEquals((byte) 3, assertInstanceOf(Byte.class, tag.get("facing")));
    }

    @Test
    void nullPropertiesAreOmitted() {
        NbtMap tag = mapper.write(new Nullable());

        assertFalse(tag.containsKey("maybe"));
        assertEquals(1, assertInstanceOf(Integer.class, tag.get("present")));
    }

    @Test
    void emptyListKeepsDeclaredElementType() {
        NbtMap tag = mapper.write(new EmptyList());

        NbtList<?> items = assertInstanceOf(NbtList.class, tag.get("items"));
        assertSame(NbtType.COMPOUND, items.getType());
        assertEquals(0, items.size());
    }

    @Test
    void nbtMapPassesThrough() {
        NbtMap extra = NbtMap.builder()
                .putShort("k", (short) 3)
                .putByte("flag", (byte) 1)
                .putList("values", NbtType.FLOAT, 0.5f)
                .build();
        Passthrough dto = new Passthrough();
        dto.extra = extra;

        NbtMap tag = mapper.write(dto);

        NbtMap written = assertInstanceOf(NbtMap.class, tag.get("extra"));
        assertEquals(extra, written);
        assertEquals((short) 3, assertInstanceOf(Short.class, written.get("k")));
        assertEquals((byte) 1, assertInstanceOf(Byte.class, written.get("flag")));
        assertSame(NbtType.FLOAT, assertInstanceOf(NbtList.class, written.get("values")).getType());
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Nested {
        @JsonProperty("value")
        int value = 9;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
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
    }

    enum Facing {
        DOWN, UP, NORTH, SOUTH;

        @JsonValue
        int id() {
            return ordinal() + 1;
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Overrides {
        @JsonProperty("color")
        @NbtTag(NbtTagType.BYTE)
        int color = 14;
        @JsonProperty("pos")
        @NbtTag(value = NbtTagType.LIST, element = NbtTagType.FLOAT)
        List<Double> pos = List.of(1.5, 2.0);
        @JsonProperty("facing")
        @NbtTag(NbtTagType.BYTE)
        Facing facing = Facing.NORTH;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Nullable {
        @JsonProperty("maybe")
        Integer maybe = null;
        @JsonProperty("present")
        Integer present = 1;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class EmptyList {
        @JsonProperty("items")
        @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
        List<Nested> items = List.of();
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Passthrough {
        @JsonProperty("extra")
        NbtMap extra;
    }
}
