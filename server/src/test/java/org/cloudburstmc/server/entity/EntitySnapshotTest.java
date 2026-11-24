package org.cloudburstmc.server.entity;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntitySnapshotTest {

    @Test
    void retainsPersistentStateWithoutCopyingIdentityPlacementOrRelationships() {
        NbtMap source = NbtMap.builder()
                .putLong("UniqueID", 42)
                .putLong("OwnerID", 99)
                .putString("Cloudburst.OwnerUUID", "12345678-1234-4321-8234-123456789abc")
                .putBoolean("Cloudburst.Tamed", true)
                .putList("Pos", NbtType.FLOAT, 1f, 2f, 3f)
                .putList("Passengers", NbtType.COMPOUND, NbtMap.EMPTY)
                .putInt("PortalCooldown", 40)
                .putString("CustomName", "Picked pig")
                .putFloat("Health", 7)
                .putBoolean("Cloudburst.FreezeLock", true)
                .build();

        NbtMap data = new CloudEntitySnapshot(EntityTypes.PIG, source).copyData();

        assertEquals("Picked pig", data.getString("CustomName"));
        assertEquals(7f, data.getFloat("Health"));
        assertTrue(data.getBoolean("Cloudburst.FreezeLock"));
        assertFalse(data.containsKey("UniqueID"));
        assertFalse(data.containsKey("OwnerID"));
        assertFalse(data.containsKey("Cloudburst.OwnerUUID"));
        assertFalse(data.containsKey("Cloudburst.Tamed"));
        assertFalse(data.containsKey("Pos"));
        assertFalse(data.containsKey("Passengers"));
        assertFalse(data.containsKey("PortalCooldown"));
        assertThrows(IllegalArgumentException.class, () -> new CloudEntitySnapshot(EntityTypes.PLAYER, NbtMap.EMPTY));
    }

    @Test
    void returnedNestedDataCannotMutateTheSnapshot() {
        NbtMap nested = NbtMap.builder().putByteArray("Bytes", new byte[]{1, 2}).build();
        CloudEntitySnapshot snapshot = new CloudEntitySnapshot(EntityTypes.PIG,
                NbtMap.builder().putCompound("Nested", nested).build());
        CloudEntitySnapshot original = new CloudEntitySnapshot(snapshot.getType(), snapshot.copyData());
        int hashCode = snapshot.hashCode();

        snapshot.copyData().getCompound("Nested").forEach((key, value) -> ((byte[]) value)[0] = 9);
        nested.forEach((key, value) -> ((byte[]) value)[0] = 8);

        assertArrayEquals(new byte[]{1, 2}, snapshot.copyData().getCompound("Nested").getByteArray("Bytes"));
        assertEquals(original, snapshot);
        assertEquals(hashCode, snapshot.hashCode());
    }

    @Test
    void copiedEntityDataSurvivesItemSerializationAndAffectsStackMatching() {
        CloudEntitySnapshot snapshot = new CloudEntitySnapshot(EntityTypes.PIG,
                NbtMap.builder().putString("CustomName", "Picked pig").putFloat("Health", 7).build());
        ItemStack plain = ItemStack.from(ItemTypes.PIG_SPAWN_EGG);
        ItemStack picked = plain.toBuilder().setData(ItemDataComponents.ENTITY_DATA, snapshot).build();

        ItemStack restored = ItemStackNbtCodec.deserializeItem(ItemStackNbtCodec.serializeItem(picked));

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
        assertEquals("Picked pig", copied.copyData().getString("CustomName"));
    }

    private static class SnapshotPig extends CloudEntity {
        private SnapshotPig() {
            super(EntityTypes.PIG, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
            this.customName(Component.text("Picked pig"));
        }
    }
}
