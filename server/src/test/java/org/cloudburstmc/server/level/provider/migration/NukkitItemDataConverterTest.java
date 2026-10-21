package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NukkitItemDataConverterTest {

    @Test
    void preservesNestedInventoryItemsAndMetadataVariants() throws IOException {
        NbtMap wool = NbtMap.builder()
                .putShort("id", (short) 35)
                .putShort("Damage", (short) 1)
                .putByte("Count", (byte) 12)
                .putByte("Slot", (byte) 4)
                .build();
        NbtMap sword = NbtMap.builder()
                .putShort("id", (short) 276)
                .putShort("Damage", (short) 37)
                .putByte("Count", (byte) 1)
                .putCompound("tag", NbtMap.builder().putString("custom", "retained").build())
                .build();
        NbtMap chest = NbtMap.builder()
                .putString("id", "Chest")
                .putList("Items", NbtType.COMPOUND, List.of(wool, sword))
                .build();

        NbtMap converted = new NukkitItemDataConverter().convert(chest);
        List<NbtMap> items = converted.getList("Items", NbtType.COMPOUND);

        assertEquals("Chest", converted.getString("id"));
        assertEquals("minecraft:orange_wool", items.getFirst().getString("Name"));
        assertEquals(0, items.getFirst().getShort("Damage"));
        assertEquals(12, items.get(0).getByte("Count"));
        assertEquals(4, items.get(0).getByte("Slot"));
        assertEquals("minecraft:diamond_sword", items.get(1).getString("Name"));
        assertEquals(37, items.get(1).getShort("Damage"));
        assertEquals("retained", items.get(1).getCompound("tag").getString("custom"));
        assertTrue(wool.containsKey("id"));
    }

    @Test
    void rejectsUnknownItemsRatherThanTurningThemIntoEmptyStacks() throws IOException {
        NbtMap item = NbtMap.builder()
                .putShort("id", (short) 30000)
                .putByte("Count", (byte) 1)
                .build();
        NukkitItemDataConverter converter = new NukkitItemDataConverter();

        assertThrows(IllegalArgumentException.class, () -> converter.convert(item));
    }
}
