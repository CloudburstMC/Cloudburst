package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntitySnapshotTest {

    @Test
    void retainsPersistentStateWithoutCopyingIdentityPlacementOrRelationships() {
        NbtMap source = NbtMap.builder()
                .putLong("UniqueID", 42)
                .putLong("OwnerID", 99)
                .putList("Pos", NbtType.FLOAT, 1f, 2f, 3f)
                .putList("Passengers", NbtType.COMPOUND, NbtMap.EMPTY)
                .putString("CustomName", "Picked pig")
                .putFloat("Health", 7)
                .build();

        NbtMap data = new CloudEntitySnapshot(EntityTypes.PIG, source).data();

        assertEquals("Picked pig", data.getString("CustomName"));
        assertEquals(7f, data.getFloat("Health"));
        assertFalse(data.containsKey("UniqueID"));
        assertFalse(data.containsKey("OwnerID"));
        assertFalse(data.containsKey("Pos"));
        assertFalse(data.containsKey("Passengers"));
        assertThrows(IllegalArgumentException.class, () -> new CloudEntitySnapshot(EntityTypes.PLAYER, NbtMap.EMPTY));
    }

    @Test
    void returnedNestedDataCannotMutateTheSnapshot() {
        NbtMap nested = NbtMap.builder().putByteArray("Bytes", new byte[]{1, 2}).build();
        CloudEntitySnapshot snapshot = new CloudEntitySnapshot(EntityTypes.PIG,
                NbtMap.builder().putCompound("Nested", nested).build());
        CloudEntitySnapshot original = new CloudEntitySnapshot(snapshot.type(), snapshot.data());
        int hashCode = snapshot.hashCode();

        snapshot.data().getCompound("Nested").forEach((key, value) -> ((byte[]) value)[0] = 9);
        nested.forEach((key, value) -> ((byte[]) value)[0] = 8);

        assertArrayEquals(new byte[]{1, 2}, snapshot.data().getCompound("Nested").getByteArray("Bytes"));
        assertEquals(original, snapshot);
        assertEquals(hashCode, snapshot.hashCode());
    }

    @Test
    void copiedEntityDataSurvivesItemSerializationAndAffectsStackMatching() {
        CloudEntitySnapshot snapshot = new CloudEntitySnapshot(EntityTypes.PIG,
                NbtMap.builder().putString("CustomName", "Picked pig").putFloat("Health", 7).build());
        ItemStack plain = ItemStack.from(ItemTypes.PIG_SPAWN_EGG);
        ItemStack picked = plain.toBuilder().setData(ItemDataComponents.ENTITY_DATA, snapshot).build();

        ItemStack restored = ItemUtils.deserializeItem(ItemUtils.serializeItem(picked));

        assertEquals(snapshot, restored.get(ItemDataComponents.ENTITY_DATA));
        assertTrue(picked.isStackableWith(restored));
        assertFalse(plain.isStackableWith(restored));
    }

    @Test
    void includesSnapshotDataOnlyWhenRequested() {
        CloudEntity entity = new SnapshotPig();

        assertFalse(entity.getPickItem(false).has(ItemDataComponents.ENTITY_DATA));
        CloudEntitySnapshot copied = assertInstanceOf(CloudEntitySnapshot.class, entity.getPickItem(true).get(ItemDataComponents.ENTITY_DATA));
        assertNotNull(copied);
        assertEquals("Picked pig", copied.data().getString("CustomName"));
    }

    private static class SnapshotPig extends CloudEntity {
        private SnapshotPig() {
            super(EntityTypes.PIG, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        @Override
        public void saveAdditionalData(NbtMapBuilder tag) {
            tag.putString("CustomName", "Picked pig");
        }
    }
}
