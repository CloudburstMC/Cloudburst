package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.ItemFrameData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the item-frame data path: every key today's load/save/saveClientData
 * handle, plus an unknown key, survive with the same NBT types.
 */
class ItemFramePersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap itemFrameTag() {
        return NbtMap.builder()
                .putString("id", "ItemFrame")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Frame")
                .putCompound("Item", ItemUtils.serializeItem(ItemStack.from(ItemTypes.DIAMOND, 1)))
                .putFloat("ItemRotation", 90.0f)
                .putFloat("ItemDropChance", 0.5f)
                .putBoolean("Fixed", true)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(ItemFrameData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.ITEM_FRAME));
        assertEquals(ItemFrameData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.GLOW_ITEM_FRAME));

        ItemFrameBlockEntity entity = (ItemFrameBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.ITEM_FRAME, POSITION);
        NbtMap input = itemFrameTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 1), entity.getItem());
        assertEquals(2, entity.getItemRotation());
        assertEquals(0.5f, entity.getItemDropChance());
        assertTrue(entity.isFixed());

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "Item", "ItemRotation", "ItemDropChance"), client.keySet());
        for (String key : Set.of("Item", "ItemRotation", "ItemDropChance")) {
            assertEquals(input.get(key), client.get(key), "client value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(client.get(key).getClass()), "client type of " + key);
        }
    }

    @Test
    void emptyItemFrameWritesDefaultsWithoutItem() {
        ItemFrameBlockEntity entity = (ItemFrameBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.ITEM_FRAME, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "ItemFrame")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertTrue(entity.getItem().isEmpty());
        assertEquals(0, entity.getItemRotation());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "ItemRotation", "ItemDropChance", "Fixed"), saved.keySet());
        assertEquals(0.0f, saved.getFloat("ItemRotation"));
        assertEquals(NbtType.FLOAT, NbtType.byClass(saved.get("ItemRotation").getClass()));
        assertEquals(1.0f, saved.getFloat("ItemDropChance"));
        assertEquals(NbtType.FLOAT, NbtType.byClass(saved.get("ItemDropChance").getClass()));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("Fixed").getClass()));

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "ItemRotation", "ItemDropChance"), client.keySet());
        assertEquals(0.0f, client.getFloat("ItemRotation"));
        assertEquals(1.0f, client.getFloat("ItemDropChance"));
    }

    @Test
    void rotationResetsWithoutItem() {
        ItemFrameBlockEntity entity = (ItemFrameBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.ITEM_FRAME, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "ItemFrame")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putFloat("ItemRotation", 90.0f)
                .putFloat("ItemDropChance", 1.0f)
                .putBoolean("Fixed", false)
                .build());

        assertEquals(0, entity.getItemRotation());
        assertEquals(0.0f, entity.getServerTag().getFloat("ItemRotation"));
    }
}
