package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.BrewingStandData;
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
 * Round-trip tests for the brewing-stand data path: every key today's load/save/saveClientData
 * handle, plus an unknown key, survive with the same NBT types.
 */
class BrewingStandPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);
    private static final int SIZE = 5;

    private static NbtMap brewingTag() {
        List<NbtMap> items = new ArrayList<>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack;
            if (slot == 0) {
                stack = ItemStack.from(ItemTypes.GOLD_NUGGET, 1);
            } else if (slot == 1) {
                stack = ItemStack.from(ItemTypes.DIAMOND, 5);
            } else if (slot == 4) {
                stack = ItemStack.from(ItemTypes.BLAZE_POWDER, 2);
            } else {
                stack = ItemStack.EMPTY;
            }
            items.add(ItemUtils.serializeItem(stack, slot));
        }
        return NbtMap.builder()
                .putString("id", "BrewingStand")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Brewer")
                .putList("Items", NbtType.COMPOUND, items)
                .putInt("CookTime", 350)
                .putInt("FuelAmount", 15)
                .putShort("FuelTotal", (short) 20)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(BrewingStandData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.BREWING_STAND));

        BrewingStandBlockEntity entity = (BrewingStandBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BREWING_STAND, POSITION);
        NbtMap input = brewingTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.GOLD_NUGGET, 1), entity.getIngredient());
        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getBottle(0));
        assertEquals(ItemStack.from(ItemTypes.BLAZE_POWDER, 2), entity.getFuel());
        assertEquals(350, entity.getCookTime());
        assertEquals(15, entity.getFuelAmount());
        assertEquals(20, entity.getFuelTotal());

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
        assertEquals(Set.of("id", "FuelAmount", "FuelTotal"), client.keySet());
        assertEquals(15, client.getInt("FuelAmount"));
        assertEquals(NbtType.INT, NbtType.byClass(client.get("FuelAmount").getClass()));
        assertEquals((short) 20, client.getShort("FuelTotal"));
        assertEquals(NbtType.SHORT, NbtType.byClass(client.get("FuelTotal").getClass()));
    }

    @Test
    void emptyBrewingStandWritesDefaultsForEverySlot() {
        BrewingStandBlockEntity entity = (BrewingStandBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BREWING_STAND, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "BrewingStand")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        NbtMap saved = entity.getServerTag();
        assertEquals(400, saved.getInt("CookTime"));
        assertEquals(NbtType.INT, NbtType.byClass(saved.get("CookTime").getClass()));
        assertEquals(0, saved.getInt("FuelAmount"));
        assertEquals(NbtType.INT, NbtType.byClass(saved.get("FuelAmount").getClass()));
        assertEquals((short) 0, saved.getShort("FuelTotal"));
        assertEquals(NbtType.SHORT, NbtType.byClass(saved.get("FuelTotal").getClass()));

        NbtList<?> items = assertInstanceOf(NbtList.class, saved.get("Items"));
        assertEquals(NbtType.COMPOUND, items.getType());
        assertEquals(SIZE, items.size());
        for (int slot = 0; slot < SIZE; slot++) {
            assertEquals(ItemUtils.serializeItem(ItemStack.EMPTY, slot), items.get(slot),
                    "slot " + slot);
        }

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "FuelAmount", "FuelTotal"), client.keySet());
        assertEquals(0, client.getInt("FuelAmount"));
        assertEquals((short) 0, client.getShort("FuelTotal"));
    }
}
