package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.ChestData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the chest data path: every key today's load/save/saveClientData
 * handle, plus an unknown key, survive with the same NBT types.
 */
class ChestPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);
    private static final int SIZE = 27;

    private static List<NbtMap> fullItems() {
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
        return items;
    }

    private static NbtMap chestTag() {
        return NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Loot chest")
                .putList("Items", NbtType.COMPOUND, fullItems())
                .putBoolean("Findable", true)
                .putString("LootTable", "minecraft:chests/simple_dungeon")
                .putInt("LootTableSeed", 12345)
                .putInt("pairx", 11)
                .putInt("pairz", -5)
                .putBoolean("pairlead", true)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(ChestData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.CHEST));

        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        NbtMap input = chestTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getItem(3));
        assertTrue(entity.isFindable());
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

        assertEquals(Set.of("id", "pairx", "pairz", "pairlead"),
                entity.getClientTag().keySet());
    }

    @Test
    void emptyChestWritesAirForEverySlot() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        NbtList<?> items = assertInstanceOf(NbtList.class, entity.getServerTag().get("Items"));
        assertEquals(NbtType.COMPOUND, items.getType());
        assertEquals(SIZE, items.size());
        for (int slot = 0; slot < SIZE; slot++) {
            assertEquals(ItemUtils.serializeItem(ItemStack.EMPTY, slot), items.get(slot),
                    "slot " + slot);
        }

        // Air entries load back as empty slots.
        for (int slot = 0; slot < SIZE; slot++) {
            assertTrue(entity.getItem(slot).isEmpty(), "slot " + slot);
        }
    }

    @Test
    void badItemFailsLoadInsteadOfEmptyingChest() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        // A suspicious stew with an out-of-range variant has no effects list,
        // so ItemUtils.deserializeItem rejects it.
        NbtMap badStew = NbtMap.builder()
                .putString("Name", "minecraft:suspicious_stew")
                .putShort("Damage", (short) 99)
                .putByte("Count", (byte) 1)
                .putByte("Slot", (byte) 0)
                .build();
        NbtMap tag = NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putList("Items", NbtType.COMPOUND, List.of(badStew))
                .build();

        assertThrows(Exception.class, () -> entity.loadAdditionalData(tag));
    }

    @Test
    void nonLeadPairedChestKeepsPairKeys() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putInt("pairx", 11)
                .putInt("pairz", -5)
                .putBoolean("pairlead", false)
                .build());

        assertTrue(entity.isPaired());

        NbtMap saved = entity.getServerTag();
        assertTrue(saved.get("pairx") instanceof Integer);
        assertEquals(11, saved.getInt("pairx"));
        assertTrue(saved.get("pairz") instanceof Integer);
        assertEquals(-5, saved.getInt("pairz"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("pairlead").getClass()));
        assertFalse(saved.getBoolean("pairlead"));

        NbtMap client = entity.getClientTag();
        assertFalse(client.containsKey("pairx"));
        assertFalse(client.containsKey("pairz"));
        assertFalse(client.containsKey("pairlead"));
    }

    @Test
    void unpairedChestDoesNotResurrectPair() throws Exception {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        entity.loadAdditionalData(chestTag());
        assertTrue(entity.isPaired());

        // unpair() itself broadcasts and looks up the pair chest, which needs a live level;
        // replicate its local state change (it clears the position but keeps pairlead)
        // so the save path can be tested standalone.
        Field pairPosition = ChestBlockEntity.class.getDeclaredField("pairPosition");
        pairPosition.setAccessible(true);
        pairPosition.set(entity, null);
        assertFalse(entity.isPaired());

        NbtMap saved = entity.getServerTag();
        assertFalse(saved.containsKey("pairx"));
        assertFalse(saved.containsKey("pairz"));
        assertFalse(saved.containsKey("pairlead"));

        NbtMap client = entity.getClientTag();
        assertFalse(client.containsKey("pairx"));
        assertFalse(client.containsKey("pairz"));
        assertFalse(client.containsKey("pairlead"));
    }

    @Test
    void lootTableRoundTrips() {
        ChestBlockEntity entity = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
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

        ChestBlockEntity plain = (ChestBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CHEST, POSITION);
        plain.loadAdditionalData(NbtMap.builder()
                .putString("id", "Chest")
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
