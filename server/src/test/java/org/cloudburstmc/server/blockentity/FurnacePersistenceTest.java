package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.FurnaceData;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the furnace data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class FurnacePersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);
    private static final int SIZE = 3;

    private static List<NbtMap> fullItems() {
        List<NbtMap> items = new ArrayList<>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack;
            if (slot == 0) {
                stack = ItemStack.from(ItemTypes.DIAMOND, 5);
            } else if (slot == 1) {
                stack = ItemStack.from(ItemTypes.STICK, 2);
            } else {
                stack = ItemStack.EMPTY;
            }
            items.add(ItemUtils.serializeItem(stack, slot));
        }
        return items;
    }

    private static NbtMap furnaceTag() {
        return NbtMap.builder()
                .putString("id", "Furnace")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Furnace")
                .putList("Items", NbtType.COMPOUND, fullItems())
                .putShort("CookTime", (short) 100)
                .putShort("BurnTime", (short) 200)
                .putShort("BurnDuration", (short) 300)
                .putString("LootTable", "minecraft:chests/simple_dungeon")
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(FurnaceData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.FURNACE));

        FurnaceBlockEntity entity = (FurnaceBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.FURNACE, POSITION);
        NbtMap input = furnaceTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getSmelting());
        assertEquals(ItemStack.from(ItemTypes.STICK, 2), entity.getFuel());
        assertTrue(entity.getResult().isEmpty());
        assertEquals(100, entity.getCookProgress());
        assertEquals(200, entity.getBurnProgress());
        assertEquals(300, entity.getBurnDuration());

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        // LootTable is not a declared furnace key; it passes through the raw-tag merge unchanged.
        assertEquals("minecraft:chests/simple_dungeon", saved.getString("LootTable"));

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void emptyFurnaceWritesDefaultsForEverySlot() {
        FurnaceBlockEntity entity = (FurnaceBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.FURNACE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Furnace")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        NbtMap saved = entity.getServerTag();
        assertEquals((short) 0, saved.getShort("CookTime"));
        assertEquals(NbtType.SHORT, NbtType.byClass(saved.get("CookTime").getClass()));
        assertEquals((short) 0, saved.getShort("BurnTime"));
        assertEquals(NbtType.SHORT, NbtType.byClass(saved.get("BurnTime").getClass()));
        assertEquals((short) 0, saved.getShort("BurnDuration"));
        assertEquals(NbtType.SHORT, NbtType.byClass(saved.get("BurnDuration").getClass()));

        NbtList<?> items = assertInstanceOf(NbtList.class, saved.get("Items"));
        assertEquals(NbtType.COMPOUND, items.getType());
        assertEquals(SIZE, items.size());
        for (int slot = 0; slot < SIZE; slot++) {
            assertEquals(ItemUtils.serializeItem(ItemStack.EMPTY, slot), items.get(slot),
                    "slot " + slot);
        }

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void blastFurnaceAndSmokerShareFurnaceData() {
        assertEquals(FurnaceData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.BLAST_FURNACE));
        assertEquals(FurnaceData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.SMOKER));

        BlastFurnaceBlockEntity blast = (BlastFurnaceBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BLAST_FURNACE, POSITION);
        NbtMap input = furnaceTag().toBuilder().putString("id", "BlastFurnace").build();
        blast.loadAdditionalData(input);

        NbtMap saved = blast.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }
        assertEquals(Set.of("id"), blast.getClientTag().keySet());

        SmokerBlockEntity smoker = (SmokerBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SMOKER, POSITION);
        NbtMap smokerInput = furnaceTag().toBuilder().putString("id", "Smoker").build();
        smoker.loadAdditionalData(smokerInput);

        NbtMap smokerSaved = smoker.getServerTag();
        Set<String> smokerExpected = new HashSet<>(smokerInput.keySet());
        smokerExpected.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(smokerExpected, smokerSaved.keySet());
        for (String key : smokerInput.keySet()) {
            assertEquals(smokerInput.get(key), smokerSaved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(smokerInput.get(key).getClass()),
                    NbtType.byClass(smokerSaved.get(key).getClass()), "type of " + key);
        }
        assertEquals(Set.of("id"), smoker.getClientTag().keySet());
    }

    @Test
    void absentKeysKeepTodayDefaults() {
        FurnaceBlockEntity entity = (FurnaceBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.FURNACE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Furnace")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        // A fresh furnace is cold and empty: every counter reads zero.
        assertEquals(0, entity.getCookProgress());
        assertEquals(0, entity.getBurnProgress());
        assertEquals(0, entity.getBurnDuration());
        for (int slot = 0; slot < SIZE; slot++) {
            assertTrue(entity.getItem(slot).isEmpty(), "slot " + slot);
        }
    }

    @Test
    void furnaceBlockEntityKeepsNoDuplicateState() {
        // Retained field and why it holds no DTO state: currentCookDuration is the live
        // smelt timer, never persisted.
        DtoStateChecks.assertKeepsNoDuplicateState(FurnaceBlockEntity.class, FurnaceData.class);
    }
}
