package org.cloudburstmc.server.nbt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtMapper;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.nbt.codec.DyeDataIntCodec;
import org.cloudburstmc.server.nbt.codec.Vector3iListCodec;
import org.cloudburstmc.server.nbt.codec.WoolColorCodec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the server's serializers for the runtime types data objects hold:
 * item containers, dye colours and positions.
 */
class ServerTypesNbtTest {

    private static final int SIZE = 27;

    private final NbtMapper mapper = ServerNbt.MAPPER;

    @Test
    void containerWritesEverySlotLikeToday() {
        Chest dto = new Chest();
        ItemStack stack = ItemStack.from(ItemTypes.DIAMOND, 5);
        dto.items.setItem(3, stack);

        NbtMap tag = mapper.write(dto);

        NbtList<?> written = assertInstanceOf(NbtList.class, tag.get("Items"));
        assertEquals(NbtType.COMPOUND, written.getType());
        List<NbtMap> expected = new ArrayList<>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) {
            expected.add(ItemUtils.serializeItem(slot == 3 ? stack : ItemStack.EMPTY, slot));
        }
        assertEquals(expected, written);
    }

    @Test
    void readIntoReplacesContainerContents() {
        Chest dto = new Chest();
        ItemContainer container = dto.items;
        dto.items.setItem(0, ItemStack.from(ItemTypes.DIRT, 32));
        dto.items.setItem(5, ItemStack.from(ItemTypes.DIRT, 32));
        ItemStack stack = ItemStack.from(ItemTypes.DIAMOND, 5);
        NbtMap tag = NbtMap.builder()
                .putList("Items", NbtType.COMPOUND, List.of(ItemUtils.serializeItem(stack, 2)))
                .build();

        mapper.readInto(tag, dto);

        assertSame(container, dto.items);
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot == 2) {
                assertEquals(stack, dto.items.getItem(slot), "slot " + slot);
            } else {
                assertTrue(dto.items.getItem(slot).isEmpty(), "slot " + slot);
            }
        }
    }

    @Test
    void readCreatesIndependentContainers() {
        NbtMap tag = NbtMap.builder()
                .putList("Items", NbtType.COMPOUND,
                        List.of(ItemUtils.serializeItem(ItemStack.from(ItemTypes.DIAMOND, 5), 2)))
                .build();

        Chest first = mapper.read(tag, Chest.class);
        Chest second = mapper.read(tag, Chest.class);

        assertNotSame(first.items, second.items);
        assertEquals(first.items.getItem(2), second.items.getItem(2));
    }

    @Test
    void mismatchLeavesContainerUntouched() {
        Chest dto = new Chest();
        dto.items.setItem(0, ItemStack.from(ItemTypes.DIRT, 32));
        NbtMap tag = NbtMap.builder().putString("Items", "oops").build();

        mapper.readInto(tag, dto);

        assertEquals(ItemStack.from(ItemTypes.DIRT, 32), dto.items.getItem(0));
    }

    @Test
    void readWithoutSizeAnnotationFails() {
        NbtMap tag = NbtMap.builder()
                .putList("Items", NbtType.COMPOUND,
                        List.of(ItemUtils.serializeItem(ItemStack.from(ItemTypes.DIAMOND, 5), 1)))
                .build();

        assertThrows(Exception.class, () -> mapper.read(tag, Unsized.class));
    }

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
    static class Chest {
        @JsonProperty("Items")
        @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
        @NbtContainerSize(SIZE)
        ItemContainer items = ItemContainers.detached(SIZE);
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Unsized {
        @JsonProperty("Items")
        @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
        ItemContainer items;
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
