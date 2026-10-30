package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.ShulkerBoxData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
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
 * Round-trip tests for the shulker box data path: every key today's load/save/saveClientData
 * handle, plus an unknown key, survive with the same NBT types.
 */
class ShulkerBoxPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);
    private static final int SIZE = 27;

    private static NbtMap shulkerBoxTag() {
        List<NbtMap> items = new ArrayList<>();
        ItemStack stack = ItemStack.from(ItemTypes.DIAMOND, 5);
        items.add(ItemUtils.serializeItem(stack, 3));
        items.add(ItemUtils.serializeItem(ItemStack.from(ItemTypes.STICK, 2), 5));
        return NbtMap.builder()
                .putString("id", "ShulkerBox")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Shulker")
                .putList("Items", NbtType.COMPOUND, items)
                .putByte("facing", (byte) Direction.SOUTH.getIndex())
                .putString("LootTable", "minecraft:chests/simple_dungeon")
                .putInt("LootTableSeed", 12345)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(ShulkerBoxData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.SHULKER_BOX));

        ShulkerBoxBlockEntity entity = (ShulkerBoxBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SHULKER_BOX, POSITION);
        NbtMap input = shulkerBoxTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getItem(3));
        assertEquals(Direction.SOUTH, entity.getFacing());
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

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "facing"), client.keySet());
        assertEquals((byte) Direction.SOUTH.getIndex(), client.getByte("facing"));
    }

    @Test
    void lootTableRoundTrips() {
        ShulkerBoxBlockEntity entity = (ShulkerBoxBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SHULKER_BOX, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "ShulkerBox")
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

        ShulkerBoxBlockEntity plain = (ShulkerBoxBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SHULKER_BOX, POSITION);
        plain.loadAdditionalData(NbtMap.builder()
                .putString("id", "ShulkerBox")
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
