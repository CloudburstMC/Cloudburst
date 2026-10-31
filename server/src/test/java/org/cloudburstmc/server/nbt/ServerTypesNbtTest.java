package org.cloudburstmc.server.nbt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.ItemContainers;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.nbt.NbtContainerSize;
import org.cloudburstmc.api.nbt.NbtMapper;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.network.NetworkUtils;
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
 * item containers.
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
    void outOfRangeSlotFailsReadWithoutTouchingContainer() {
        Chest dto = new Chest();
        dto.items.setItem(0, ItemStack.from(ItemTypes.DIRT, 32));
        dto.items.setItem(5, ItemStack.from(ItemTypes.DIRT, 32));
        NbtMap tag = NbtMap.builder()
                .putList("Items", NbtType.COMPOUND,
                        List.of(ItemUtils.serializeItem(ItemStack.from(ItemTypes.DIAMOND, 5), SIZE)))
                .build();

        // An out-of-range Slot fails the read with a bare exception, as the old bounds
        // check did, instead of keeping the current contents with a warning (which would
        // lose the stored items on the next save). Staging validates before mutating,
        // so the live container is untouched.
        tools.jackson.core.JacksonException thrown =
                assertThrows(tools.jackson.core.JacksonException.class, () -> mapper.readInto(tag, dto));
        boolean bare = false;
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            if (cause instanceof IndexOutOfBoundsException) {
                bare = true;
            }
        }
        assertTrue(bare, "out-of-range slot must fail with a bare exception, not a kind mismatch");

        assertEquals(ItemStack.from(ItemTypes.DIRT, 32), dto.items.getItem(0));
        assertEquals(ItemStack.from(ItemTypes.DIRT, 32), dto.items.getItem(5));
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot != 0 && slot != 5) {
                assertTrue(dto.items.getItem(slot).isEmpty(), "slot " + slot);
            }
        }
    }

    @Test
    void unreadableItemLeavesContainerUnchanged() {
        Chest dto = new Chest();
        dto.items.setItem(0, ItemStack.from(ItemTypes.DIRT, 32));
        NbtMap bad = NbtMap.builder()
                .putString("Name", "!!! not an identifier !!!")
                .putByte("Count", (byte) 1)
                .putByte("Slot", (byte) 2)
                .build();
        NbtMap tag = NbtMap.builder()
                .putList("Items", NbtType.COMPOUND, List.of(bad))
                .build();

        assertThrows(Exception.class, () -> mapper.readInto(tag, dto));

        assertEquals(ItemStack.from(ItemTypes.DIRT, 32), dto.items.getItem(0));
        for (int slot = 1; slot < SIZE; slot++) {
            assertTrue(dto.items.getItem(slot).isEmpty(), "slot " + slot);
        }
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
    void effectTypeWritesNetworkIdAsInt() {
        Effects dto = new Effects();
        dto.primary = EffectTypes.SPEED;

        NbtMap tag = mapper.write(dto);

        assertEquals((int) NetworkUtils.effectToNetwork(EffectTypes.SPEED), ((Number) tag.get("primary")).intValue());
        assertEquals(NbtType.INT, NbtType.byClass(tag.get("primary").getClass()));
    }

    @Test
    void effectTypeRoundTrip() {
        Effects dto = new Effects();
        NbtMap tag = NbtMap.builder()
                .putInt("primary", NetworkUtils.effectToNetwork(EffectTypes.REGENERATION))
                .build();

        mapper.readInto(tag, dto);

        assertEquals(EffectTypes.REGENERATION, dto.primary);
    }

    @Test
    void effectTypeWrongKindKeepsCurrent() {
        Effects dto = new Effects();
        dto.primary = EffectTypes.SPEED;
        NbtMap tag = NbtMap.builder().putString("primary", "speed").build();

        mapper.readInto(tag, dto);

        assertEquals(EffectTypes.SPEED, dto.primary);
    }

    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    static class Effects {
        @JsonProperty("primary")
        EffectType primary;
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
}
