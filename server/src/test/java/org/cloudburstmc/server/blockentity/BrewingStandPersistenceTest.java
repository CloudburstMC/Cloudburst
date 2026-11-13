package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.data.CloudBrewingStandData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
            items.add(ItemStackNbtCodec.serializeItem(stack, slot));
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
        assertEquals(CloudBrewingStandData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.BREWING_STAND));

        BrewingStandBlockEntity entity = (BrewingStandBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BREWING_STAND, POSITION);
        NbtMap input = brewingTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.GOLD_NUGGET, 1), entity.getIngredient());
        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 5), entity.getBottle(0));
        assertEquals(ItemStack.from(ItemTypes.BLAZE_POWDER, 2), entity.getFuel());
        assertEquals(350, entity.data().getCookTime());
        assertEquals(15, entity.getFuelLevel());
        assertEquals(20, entity.data().getFuelTotal());

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z", "BrewDuration"));
        assertEquals(expectedKeys, saved.keySet());
        assertEquals(400, saved.getInt("BrewDuration"));
        for (String key : input.keySet()) {
            if (key.equals("CookTime") || key.equals("FuelAmount")) {
                assertEquals((short) input.getInt(key), saved.getShort(key));
                continue;
            }
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "FuelAmount", "FuelTotal"), client.keySet());
        assertEquals(15, client.getShort("FuelAmount"));
        assertEquals(NbtType.SHORT, NbtType.byClass(client.get("FuelAmount").getClass()));
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
        assertEquals(400, saved.getShort("CookTime"));
        assertEquals(NbtType.SHORT, NbtType.byClass(saved.get("CookTime").getClass()));
        assertEquals(0, saved.getShort("FuelAmount"));
        assertEquals(NbtType.SHORT, NbtType.byClass(saved.get("FuelAmount").getClass()));
        assertEquals((short) 0, saved.getShort("FuelTotal"));
        assertEquals(NbtType.SHORT, NbtType.byClass(saved.get("FuelTotal").getClass()));

        NbtList<?> items = assertInstanceOf(NbtList.class, saved.get("Items"));
        assertEquals(NbtType.COMPOUND, items.getType());
        assertEquals(SIZE, items.size());
        for (int slot = 0; slot < SIZE; slot++) {
            assertEquals(ItemStackNbtCodec.serializeItem(ItemStack.EMPTY, slot), items.get(slot),
                    "slot " + slot);
        }

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "FuelAmount", "FuelTotal"), client.keySet());
        assertEquals(0, client.getShort("FuelAmount"));
        assertEquals((short) 0, client.getShort("FuelTotal"));
    }

    @Test
    void absentKeysKeepTodayDefaults() {
        BrewingStandBlockEntity entity = (BrewingStandBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BREWING_STAND, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "BrewingStand")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertEquals(400, entity.data().getCookTime());
        assertEquals(0, entity.getFuelLevel());
        assertEquals((short) 0, entity.data().getFuelTotal());
        assertTrue(entity.getIngredient().isEmpty());
        assertTrue(entity.getFuel().isEmpty());
        for (int slot = 0; slot < 3; slot++) {
            assertTrue(entity.getBottle(slot).isEmpty(), "bottle " + slot);
        }
    }

    @Test
    void brewingStandBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(BrewingStandBlockEntity.class, CloudBrewingStandData.class);
    }
}
