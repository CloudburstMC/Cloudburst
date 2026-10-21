package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.data.serializer.DyedColorSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DyedColorSerializerTest {

    @Test
    void equipmentColorRoundTripsWithAnOpaqueAlphaChannel() {
        DyedColorSerializer serializer = new DyedColorSerializer();
        ItemStack item = ItemStack.builder(ItemTypes.LEATHER_CHESTPLATE).setData(ItemDataComponents.DYED_COLOR, 0x123456).build();
        NbtMapBuilder tag = NbtMap.builder();

        serializer.serialize(item, tag, 0x123456);
        NbtMap stored = tag.build();

        assertEquals(0xFF123456, stored.getInt("customColor"));
        assertEquals(0x123456, serializer.deserialize(ItemTypes.LEATHER_CHESTPLATE.getId(), stored));
        assertEquals(0x123456, serializer.deserialize(Identifier.parse("test:dyed_equipment"), stored));
    }

    @Test
    void fireworkStarColorDoesNotBecomeEquipmentDye() {
        DyedColorSerializer serializer = new DyedColorSerializer();
        NbtMap tag = NbtMap.builder().putInt("customColor", 0xFF123456).build();

        assertNull(serializer.deserialize(ItemTypes.FIREWORK_STAR.getId(), tag));
        assertNull(serializer.deserialize(ItemTypes.LEATHER_HELMET.getId(), NbtMap.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize(ItemStack.from(ItemTypes.LEATHER_HELMET), NbtMap.builder(), 0xFF123456));
    }
}
