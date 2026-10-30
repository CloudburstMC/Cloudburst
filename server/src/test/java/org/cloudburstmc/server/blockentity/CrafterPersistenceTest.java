package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.CrafterData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the crafter data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class CrafterPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);
    private static final int SIZE = 9;

    private static NbtMap crafterTag() {
        List<NbtMap> items = new ArrayList<>();
        ItemStack stack = ItemStack.from(ItemTypes.DIAMOND, 5);
        items.add(ItemUtils.serializeItem(stack, 3));
        items.add(ItemUtils.serializeItem(ItemStack.from(ItemTypes.STICK, 2), 5));
        return NbtMap.builder()
                .putString("id", "Crafter")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Crafter")
                .putList("Items", NbtType.COMPOUND, items)
                .putShort("DisabledSlots", (short) 0b101)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(CrafterData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.CRAFTER));

        CrafterBlockEntity entity = (CrafterBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CRAFTER, POSITION);
        NbtMap input = crafterTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getItem(3));
        assertTrue(entity.isSlotDisabled(0));
        assertFalse(entity.isSlotDisabled(1));
        assertTrue(entity.isSlotDisabled(2));

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }
}
