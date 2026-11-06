package org.cloudburstmc.api.nbt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.nbt.NbtMap;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NbtCodecTest {

    private final NbtMapper mapper = NbtMapper.create();

    @Test
    void codecOverridesTypeBothWays() {
        Painted dto = new Painted();
        dto.color = Color.MAGENTA;

        NbtMap tag = mapper.write(dto);

        assertEquals((byte) 2, assertInstanceOf(Byte.class, tag.get("Color")));
        assertEquals(Color.MAGENTA, mapper.read(tag, Painted.class).color);
    }

    @Test
    void codecIsChosenPerField() {
        Painted dto = new Painted();
        dto.color = Color.ORANGE;
        dto.base = Color.MAGENTA;

        NbtMap tag = mapper.write(dto);

        assertEquals((byte) 1, assertInstanceOf(Byte.class, tag.get("Color")));
        assertEquals(2, assertInstanceOf(Integer.class, tag.get("Base")));
        Painted read = mapper.read(tag, Painted.class);
        assertEquals(Color.ORANGE, read.color);
        assertEquals(Color.MAGENTA, read.base);
    }

    @Test
    void codecReadsAnyNumericWidthItAccepts() {
        NbtMap tag = NbtMap.builder().putInt("Color", 1).build();

        assertEquals(Color.ORANGE, mapper.read(tag, Painted.class).color);
    }

    @Test
    void codecFieldOwnedKeyStillOwned() {
        assertEquals(Set.of("Color", "Base", "Count"), mapper.ownedKeys(Painted.class));
    }

    @Test
    void codecWrongKindKeepsCurrent() {
        Painted dto = new Painted();
        dto.color = Color.ORANGE;
        NbtMap tag = NbtMap.builder()
                .putString("Color", "magenta")
                .putCompound("Base", NbtMap.builder().putInt("a", 1).build())
                .putInt("Count", 4)
                .build();

        mapper.readInto(tag, dto);

        assertEquals(Color.ORANGE, dto.color);
        assertEquals(Color.WHITE, dto.base);
        assertEquals(4, dto.count);
    }

    @Test
    void codecClassCastExceptionPropagates() {
        Buggy dto = new Buggy();
        dto.color = Color.MAGENTA;
        NbtMap tag = NbtMap.builder().putByte("Color", (byte) 1).build();

        // A cast bug in the codec is not a kind mismatch: the read fails instead of
        // keeping a value that the next save would write over the stored one
        JacksonException thrown = assertThrows(JacksonException.class, () -> mapper.readInto(tag, dto));
        assertTrue(hasCause(thrown, ClassCastException.class));
        assertFalse(hasCause(thrown, NbtKindMismatchException.class));
        assertEquals(Color.MAGENTA, dto.color);
    }

    @Test
    void codecFailureFailsTheRead() {
        NbtMap tag = NbtMap.builder().putByte("Color", (byte) 99).build();

        JacksonException thrown = assertThrows(JacksonException.class, () -> mapper.read(tag, Painted.class));
        assertTrue(hasCause(thrown, IllegalArgumentException.class));
    }

    @Test
    void codecCanWriteCompound() {
        Placed dto = new Placed();
        dto.at = new Point(3, -4);
        dto.after = 6;

        NbtMap tag = mapper.write(dto);

        assertEquals(NbtMap.builder().putInt("X", 3).putInt("Y", -4).build(), tag.get("At"));
        Placed read = mapper.read(tag, Placed.class);
        assertEquals(new Point(3, -4), read.at);
        // The parser continues after the compound the codec read
        assertEquals(6, read.after);
    }

    @Test
    void codecFieldIsWrittenInClientView() {
        Painted dto = new Painted();
        dto.color = Color.MAGENTA;

        NbtMap tag = mapper.writeClient(dto);

        assertEquals(Set.of("Color"), tag.keySet());
        assertEquals((byte) 2, assertInstanceOf(Byte.class, tag.get("Color")));
    }

    @Test
    void codecAppliesToNbtOnly() {
        Painted dto = new Painted();
        dto.color = Color.MAGENTA;

        String json = JsonMapper.builder().build().writeValueAsString(dto);

        assertTrue(json.contains("\"Color\":\"MAGENTA\""), json);
    }

    @Test
    void codecMustWriteAnNbtValue() {
        Broken dto = new Broken();

        assertThrows(JacksonException.class, () -> mapper.write(dto));
    }

    @Test
    void codecIsCreatedOncePerClass() {
        Counted dto = new Counted();
        NbtMapper other = NbtMapper.create();

        NbtMap tag = mapper.write(dto);
        other.write(dto);
        mapper.read(tag, Counted.class);
        other.read(tag, Counted.class);

        assertEquals(1, CountingCodec.CREATED.get());
    }

    private static boolean hasCause(Throwable thrown, Class<? extends Throwable> type) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }

    enum Color {
        WHITE, ORANGE, MAGENTA
    }

    /**
     * Writes a colour as its ordinal in a byte.
     */
    static final class OrdinalByteCodec implements NbtValueCodec<Color> {
        @Override
        public Object toNbt(Color value) {
            return (byte) value.ordinal();
        }

        @Override
        public Color fromNbt(Object nbt) {
            if (!(nbt instanceof Number number)) {
                throw new NbtKindMismatchException("Expected a number, got " + nbt.getClass().getSimpleName());
            }
            int ordinal = number.intValue();
            if (ordinal < 0 || ordinal >= Color.values().length) {
                throw new IllegalArgumentException("No colour " + ordinal);
            }
            return Color.values()[ordinal];
        }
    }

    /**
     * Writes a colour as its ordinal in an int.
     */
    static final class OrdinalIntCodec implements NbtValueCodec<Color> {
        @Override
        public Object toNbt(Color value) {
            return value.ordinal();
        }

        @Override
        public Color fromNbt(Object nbt) {
            if (!(nbt instanceof Integer ordinal)) {
                throw new NbtKindMismatchException("Expected an int, got " + nbt.getClass().getSimpleName());
            }
            return Color.values()[ordinal];
        }
    }

    /**
     * Reads a byte correctly typed but casts it wrongly: a bug, not a stored value of the
     * wrong kind.
     */
    static final class BuggyCastCodec implements NbtValueCodec<Color> {
        @Override
        public Object toNbt(Color value) {
            return (byte) value.ordinal();
        }

        @Override
        public Color fromNbt(Object nbt) {
            if (!(nbt instanceof Byte)) {
                throw new NbtKindMismatchException("Expected a byte");
            }
            return Color.valueOf((String) nbt);
        }
    }

    record Point(int x, int y) {
    }

    static final class PointCodec implements NbtValueCodec<Point> {
        @Override
        public Object toNbt(Point value) {
            return NbtMap.builder().putInt("X", value.x()).putInt("Y", value.y()).build();
        }

        @Override
        public Point fromNbt(Object nbt) {
            NbtMap tag = (NbtMap) nbt;
            return new Point(tag.getInt("X"), tag.getInt("Y"));
        }
    }

    static final class NotNbtCodec implements NbtValueCodec<Color> {
        @Override
        public Object toNbt(Color value) {
            return new Object();
        }

        @Override
        public Color fromNbt(Object nbt) {
            return Color.WHITE;
        }
    }

    static final class CountingCodec implements NbtValueCodec<Color> {
        static final AtomicInteger CREATED = new AtomicInteger();

        CountingCodec() {
            CREATED.incrementAndGet();
        }

        @Override
        public Object toNbt(Color value) {
            return (byte) value.ordinal();
        }

        @Override
        public Color fromNbt(Object nbt) {
            return Color.values()[((Number) nbt).intValue()];
        }
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Painted {
        @JsonProperty("Color")
        @NbtCodec(OrdinalByteCodec.class)
        @NbtClient
        Color color = Color.WHITE;
        @JsonProperty("Base")
        @NbtCodec(OrdinalIntCodec.class)
        Color base = Color.WHITE;
        @JsonProperty("Count")
        int count;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Placed {
        @JsonProperty("Size")
        int after;
        @JsonProperty("At")
        @NbtCodec(PointCodec.class)
        Point at;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Buggy {
        @JsonProperty("Color")
        @NbtCodec(BuggyCastCodec.class)
        Color color = Color.WHITE;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Broken {
        @JsonProperty("Color")
        @NbtCodec(NotNbtCodec.class)
        Color color = Color.WHITE;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Counted {
        @JsonProperty("First")
        @NbtCodec(CountingCodec.class)
        Color first = Color.ORANGE;
        @JsonProperty("Second")
        @NbtCodec(CountingCodec.class)
        Color second = Color.MAGENTA;
    }
}
