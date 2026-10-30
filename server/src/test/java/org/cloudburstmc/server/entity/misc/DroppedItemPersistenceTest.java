package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.DroppedItemData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the dropped-item data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class DroppedItemPersistenceTest {

    private static final Set<String> BASE_KEYS = Set.of(
            "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air",
            "OnGround", "Invulnerable", "Scale", "Tags");

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(DroppedItemData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.ITEM));

        EntityDroppedItem item = (EntityDroppedItem) PersistenceFixtures.entity(EntityTypes.ITEM);
        ItemStack stack = ItemStack.from(ItemTypes.DIAMOND, 1);
        NbtMap input = NbtMap.builder()
                .putShort("Health", (short) 3)
                .putShort("PickupDelay", (short) 20)
                .putShort("Age", (short) 30)
                .putLong("OwnerID", 123456789L)
                .putCompound("Item", ItemUtils.serializeItem(stack))
                .putInt("Mystery", 7)
                .build();
        item.loadAdditionalData(input);

        assertEquals(3, item.getHealth());
        assertEquals(20, item.getPickupDelay());
        assertEquals(stack, item.getItem());

        NbtMap saved = save(item);

        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(BASE_KEYS);
        expectedKeys.add("identifier");
        assertEquals(expectedKeys, saved.keySet());

        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }
        assertEquals(123456789L, saved.getLong("OwnerID"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityDroppedItem item = (EntityDroppedItem) PersistenceFixtures.entity(EntityTypes.ITEM);

        NbtMap saved = save(item);

        Set<String> expectedKeys = new HashSet<>(BASE_KEYS);
        expectedKeys.addAll(Set.of("Health", "PickupDelay", "Age", "OwnerID", "Item", "identifier"));
        assertEquals(expectedKeys, saved.keySet());

        // Today's save always writes the capped health, zero delays and age, no owner and an air item.
        assertTrue(saved.get("Health") instanceof Short, "Health must be a short");
        assertEquals((short) 5, saved.getShort("Health"));
        assertTrue(saved.get("PickupDelay") instanceof Short, "PickupDelay must be a short");
        assertEquals((short) 0, saved.getShort("PickupDelay"));
        assertTrue(saved.get("Age") instanceof Short, "Age must be a short");
        assertEquals((short) 0, saved.getShort("Age"));
        assertTrue(saved.get("OwnerID") instanceof Long, "OwnerID must be a long");
        assertEquals(-1L, saved.getLong("OwnerID"));
        assertEquals(ItemUtils.serializeItem(ItemStack.EMPTY), saved.getCompound("Item"));
    }

    @Test
    void absentKeysLeaveStateUnchanged() {
        EntityDroppedItem item = (EntityDroppedItem) PersistenceFixtures.entity(EntityTypes.ITEM);
        ItemStack stack = ItemStack.from(ItemTypes.DIAMOND, 1);
        item.loadAdditionalData(NbtMap.builder()
                .putShort("Health", (short) 3)
                .putShort("PickupDelay", (short) 20)
                .putShort("Age", (short) 30)
                .putLong("OwnerID", 42L)
                .putCompound("Item", ItemUtils.serializeItem(stack))
                .build());

        item.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(3, item.getHealth());
        assertEquals(20, item.getPickupDelay());
        assertEquals(stack, item.getItem());

        NbtMap saved = save(item);
        assertEquals((short) 30, saved.getShort("Age"));
        assertEquals(42L, saved.getLong("OwnerID"));
    }
}
