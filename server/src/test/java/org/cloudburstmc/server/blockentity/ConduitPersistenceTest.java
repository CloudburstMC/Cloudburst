package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.data.CloudBlockEntityData;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConduitPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    @Test
    void roundTripStripsTargetAndActiveFromServerTag() {
        assertEquals(CloudBlockEntityData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.CONDUIT));

        ConduitBlockEntity entity = (ConduitBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CONDUIT, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Conduit")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Conduit")
                .putBoolean("Active", true)
                .putLong("Target", 123L)
                .putInt("Mystery", 7)
                .build());

        assertFalse(entity.isActive());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "CustomName", "Mystery"), saved.keySet());
        assertEquals(7, saved.getInt("Mystery"));

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "Active", "Target"), client.keySet());
        assertFalse(client.getBoolean("Active"));
        assertEquals(-1L, client.getLong("Target"));
        assertEquals(NbtType.BYTE, NbtType.byClass(client.get("Active").getClass()));
        assertEquals(NbtType.LONG, NbtType.byClass(client.get("Target").getClass()));
    }

    @Test
    void defaultConduitWritesClientStateOnly() {
        ConduitBlockEntity entity = (ConduitBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.CONDUIT, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Conduit")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
        assertEquals(Set.of("id", "Active", "Target"), entity.getClientTag().keySet());

        Set<String> keys = new HashSet<>(entity.getServerTag().keySet());
        assertFalse(keys.contains("Target"));
        assertFalse(keys.contains("Active"));
    }

    @Test
    void conduitBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(ConduitBlockEntity.class, CloudBlockEntityData.class);
    }
}
