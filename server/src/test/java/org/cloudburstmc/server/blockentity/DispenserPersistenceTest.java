package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.DispenserData;
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
 * Round-trip tests for the dispenser data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class DispenserPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);
    private static final int SIZE = 9;

    private static NbtMap dispenserTag() {
        List<NbtMap> items = new ArrayList<>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack;
            if (slot == 3) {
                stack = ItemStack.from(ItemTypes.DIAMOND, 5);
            } else if (slot == 5) {
                stack = ItemStack.from(ItemTypes.STICK, 2);
            } else {
                stack = ItemStack.EMPTY;
            }
            items.add(ItemUtils.serializeItem(stack, slot));
        }
        return NbtMap.builder()
                .putString("id", "Dispenser")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Dispenser")
                .putList("Items", NbtType.COMPOUND, items)
                .putString("LootTable", "minecraft:chests/simple_dungeon")
                .putInt("LootTableSeed", 12345)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(DispenserData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.DISPENSER));

        DispenserBlockEntity entity = (DispenserBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.DISPENSER, POSITION);
        NbtMap input = dispenserTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getItem(3));
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
        DispenserBlockEntity entity = (DispenserBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.DISPENSER, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Dispenser")
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

        DispenserBlockEntity plain = (DispenserBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.DISPENSER, POSITION);
        plain.loadAdditionalData(NbtMap.builder()
                .putString("id", "Dispenser")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertNull(plain.getLootTable());
        assertNull(plain.getLootTableSeed());
        assertFalse(plain.getServerTag().containsKey("LootTable"));
        assertFalse(plain.getServerTag().containsKey("LootTableSeed"));
    }

    @Test
    void dispenserBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(DispenserBlockEntity.class, DispenserData.class);
    }
}
