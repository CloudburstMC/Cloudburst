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

import java.lang.reflect.Field;
import java.util.Arrays;
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
    void campfireClearedSlotIsNotWritten() throws Exception {
        CampfireBlockEntity entity = (CampfireBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CAMPFIRE, POSITION);
        entity.loadAdditionalData(campfireTag());

        // clearItems() itself broadcasts, which needs a live level; replicate its
        // local state change so the save path can be tested standalone.
        Field items = CampfireBlockEntity.class.getDeclaredField("items");
        items.setAccessible(true);
        Arrays.fill((ItemStack[]) items.get(entity), null);
        Field itemTimes = CampfireBlockEntity.class.getDeclaredField("itemTimes");
        itemTimes.setAccessible(true);
        Arrays.fill((int[]) itemTimes.get(entity), 0);

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
}
