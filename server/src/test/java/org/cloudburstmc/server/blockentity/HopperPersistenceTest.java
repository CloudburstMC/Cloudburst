package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.HopperData;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the hopper data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class HopperPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);
    private static final int SIZE = 5;

    private static NbtMap hopperTag() {
        List<NbtMap> items = new ArrayList<>();
        ItemStack stack = ItemStack.from(ItemTypes.DIAMOND, 5);
        items.add(ItemUtils.serializeItem(stack, 2));
        items.add(ItemUtils.serializeItem(ItemStack.from(ItemTypes.STICK, 2), 4));
        return NbtMap.builder()
                .putString("id", "Hopper")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Hopper")
                .putList("Items", NbtType.COMPOUND, items)
                .putInt("TransferCooldown", 5)
                .putString("LootTable", "minecraft:chests/simple_dungeon")
                .putInt("LootTableSeed", 12345)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(HopperData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.HOPPER));

        HopperBlockEntity entity = (HopperBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.HOPPER, POSITION);
        NbtMap input = hopperTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getItem(2));
        assertEquals(5, entity.getTransferCooldown());
        assertEquals("minecraft:chests/simple_dungeon", entity.getLootTable());
        assertEquals(12345, entity.getLootTableSeed());

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

    @Test
    void lootTableRoundTrips() {
        HopperBlockEntity entity = (HopperBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.HOPPER, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Hopper")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putString("LootTable", "minecraft:chests/simple_dungeon")
                .putInt("LootTableSeed", 12345)
                .build());

        assertEquals("minecraft:chests/simple_dungeon", entity.getLootTable());
        assertEquals(12345, entity.getLootTableSeed());

        NbtMap saved = entity.getServerTag();
        assertEquals("minecraft:chests/simple_dungeon", saved.getString("LootTable"));
        assertTrue(saved.get("LootTableSeed") instanceof Integer);
        assertEquals(12345, saved.getInt("LootTableSeed"));
        assertFalse(entity.getClientTag().containsKey("LootTable"));
        assertFalse(entity.getClientTag().containsKey("LootTableSeed"));

        HopperBlockEntity plain = (HopperBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.HOPPER, POSITION);
        plain.loadAdditionalData(NbtMap.builder()
                .putString("id", "Hopper")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertNull(plain.getLootTable());
        assertNull(plain.getLootTableSeed());
        assertFalse(plain.getServerTag().containsKey("LootTable"));
        assertFalse(plain.getServerTag().containsKey("LootTableSeed"));
    }
}
