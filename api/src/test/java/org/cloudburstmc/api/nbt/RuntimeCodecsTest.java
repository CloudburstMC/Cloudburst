package org.cloudburstmc.api.nbt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.nbt.codec.DyeDataIntCodec;
import org.cloudburstmc.api.nbt.codec.Vector3iListCodec;
import org.cloudburstmc.api.nbt.codec.WoolColorCodec;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Round-trips for the runtime-type codecs: wool colours, dye colours and positions.
 */
class RuntimeCodecsTest {

    private final NbtMapper mapper = NbtMapper.create();

    @Test
    void woolColorRoundTrips() {
        Dyed dto = new Dyed();
        dto.color = DyeColor.RED;

        NbtMap tag = mapper.write(dto);

        assertEquals((byte) 14, assertInstanceOf(Byte.class, tag.get("Color")));
        assertEquals(DyeColor.RED, mapper.read(tag, Dyed.class).color);
    }

    @Test
    void dyeDataIntRoundTrips() {
        Based dto = new Based();
        dto.base = DyeColor.BROWN;

        NbtMap tag = mapper.write(dto);

        assertEquals(DyeColor.BROWN.getDyeData(), assertInstanceOf(Integer.class, tag.get("Base")));
        assertEquals(DyeColor.BROWN, mapper.read(tag, Based.class).base);
    }

    @Test
    void vector3iListRoundTrips() {
        Placed dto = new Placed();
        dto.exit = Vector3i.from(10, 64, -5);

        NbtMap tag = mapper.write(dto);

        NbtList<?> written = assertInstanceOf(NbtList.class, tag.get("ExitPortal"));
        assertEquals(NbtType.INT, written.getType());
        assertEquals(List.of(10, 64, -5), written);
        assertEquals(Vector3i.from(10, 64, -5), mapper.read(tag, Placed.class).exit);
    }

    @Test
    void codecWrongKindKeepsCurrent() {
        Dyed dto = new Dyed();
        dto.color = DyeColor.RED;

        mapper.readInto(NbtMap.builder().putString("Color", "red").build(), dto);

        assertEquals(DyeColor.RED, dto.color);
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Dyed {
        @JsonProperty("Color")
        @NbtCodec(WoolColorCodec.class)
        DyeColor color = DyeColor.WHITE;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Based {
        @JsonProperty("Base")
        @NbtCodec(DyeDataIntCodec.class)
        DyeColor base = DyeColor.WHITE;
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Placed {
        @JsonProperty("ExitPortal")
        @NbtCodec(Vector3iListCodec.class)
        Vector3i exit = Vector3i.ZERO;
    }
}
