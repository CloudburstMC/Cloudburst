package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.CampfireData;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the campfire data path: every key today's load/saveClientData handle,
 * plus an unknown key, survive with the same NBT types.
 */
class CampfirePersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap campfireTag() {
        return NbtMap.builder()
                .putString("id", "Campfire")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Campfire")
                .putCompound("Item1", ItemUtils.serializeItem(ItemStack.from(ItemTypes.DIAMOND, 1)))
                .putInt("ItemTime1", 100)
                .putCompound("Item3", ItemUtils.serializeItem(ItemStack.from(ItemTypes.STICK, 1)))
                .putInt("ItemTime3", 50)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(CampfireData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.CAMPFIRE));

        CampfireBlockEntity entity = (CampfireBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CAMPFIRE, POSITION);
        NbtMap input = campfireTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 1), entity.getItemInFire(0));
        assertNull(entity.getItemInFire(1));
        assertEquals(ItemStack.from(ItemTypes.STICK, 1), entity.getItemInFire(2));
        assertNull(entity.getItemInFire(3));

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
        assertEquals(Set.of("id", "Item1", "ItemTime1", "Item3", "ItemTime3"), client.keySet());
        for (String key : Set.of("Item1", "ItemTime1", "Item3", "ItemTime3")) {
            assertEquals(input.get(key), client.get(key), "client value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(client.get(key).getClass()), "client type of " + key);
        }
    }

    @Test
    void emptyCampfireWritesNoItemKeys() {
        CampfireBlockEntity entity = (CampfireBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CAMPFIRE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Campfire")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
        assertTrue(entity.getServerTag().getBoolean("isMovable"));

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void campfireClearedSlotIsNotWritten() {
        CampfireBlockEntity entity = (CampfireBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CAMPFIRE, POSITION);
        entity.loadAdditionalData(campfireTag());

        // clearItems() itself broadcasts, which needs a live level; replicate its
        // local state change through the DTO so the save path can be tested standalone.
        CampfireData data = (CampfireData) entity.data();
        data.setItem1(null);
        data.setItemTime1(null);
        data.setItem2(null);
        data.setItemTime2(null);
        data.setItem3(null);
        data.setItemTime3(null);
        data.setItem4(null);
        data.setItemTime4(null);

        NbtMap saved = entity.getServerTag();
        for (String key : Set.of("Item1", "Item2", "Item3", "Item4",
                "ItemTime1", "ItemTime2", "ItemTime3", "ItemTime4")) {
            assertFalse(saved.containsKey(key), "server tag still has " + key);
        }
        assertNull(entity.getItemInFire(0));
        assertNull(entity.getItemInFire(2));

        NbtMap client = entity.getClientTag();
        for (String key : Set.of("Item1", "Item2", "Item3", "Item4",
                "ItemTime1", "ItemTime2", "ItemTime3", "ItemTime4")) {
            assertFalse(client.containsKey(key), "client tag still has " + key);
        }
    }

    @Test
    void absentKeysKeepTodayDefaults() {
        CampfireBlockEntity entity = (CampfireBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CAMPFIRE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Campfire")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        // A fresh campfire holds nothing; every slot and time key stays absent.
        for (int slot = 0; slot < 4; slot++) {
            assertNull(entity.getItemInFire(slot), "slot " + slot);
        }
        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void unpairedTimeKeysAreNormalized() {
        CampfireBlockEntity entity = (CampfireBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CAMPFIRE, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Campfire")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putCompound("Item1", ItemUtils.serializeItem(ItemStack.from(ItemTypes.DIAMOND, 1)))
                .putInt("ItemTime2", 50)
                .build());

        // An item without its time cooks from zero; a time without its item is
        // dropped. The save writes exactly the keys it used to.
        assertEquals(ItemStack.from(ItemTypes.DIAMOND, 1), entity.getItemInFire(0));
        assertNull(entity.getItemInFire(1));

        NbtMap saved = entity.getServerTag();
        assertEquals(0, saved.getInt("ItemTime1"));
        assertFalse(saved.containsKey("Item2"), "server tag gained Item2");
        assertFalse(saved.containsKey("ItemTime2"), "server tag gained ItemTime2");
    }

    @Test
    void campfireBlockEntityKeepsNoDuplicateState() {
        // No retained fields: every slot and its time live in CampfireData.
        DtoStateChecks.assertKeepsNoDuplicateState(CampfireBlockEntity.class, CampfireData.class);
    }
}
