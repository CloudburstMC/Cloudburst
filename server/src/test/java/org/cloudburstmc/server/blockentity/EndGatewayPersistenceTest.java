package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.EndGatewayData;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the end-gateway data path: every key today's load/save
 * handle, plus an unknown key, survive with the same NBT types. The client carries
 * the age as an int and always carries the exit portal.
 */
class EndGatewayPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap endGatewayTag() {
        return NbtMap.builder()
                .putString("id", "EndGateway")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Gateway")
                .putLong("Age", 123456789L)
                .putBoolean("ExactTeleport", true)
                .putList("ExitPortal", NbtType.INT, List.of(10, 64, -5))
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(EndGatewayData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.END_GATEWAY));

        EndGatewayBlockEntity entity = (EndGatewayBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.END_GATEWAY, POSITION);
        NbtMap input = endGatewayTag();
        entity.loadAdditionalData(input);

        assertEquals(123456789L, entity.getAge());
        assertTrue(entity.isExactTeleport());

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
        assertEquals(Set.of("id", "Age", "ExitPortal"), client.keySet());
        assertEquals(123456789, client.getInt("Age"));
        assertEquals(NbtType.INT, NbtType.byClass(client.get("Age").getClass()));
        assertEquals(List.of(10, 64, -5), client.getList("ExitPortal", NbtType.INT));
    }

    @Test
    void defaultGatewayOmitsExitPortalFromServerTag() {
        EndGatewayBlockEntity entity = (EndGatewayBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.END_GATEWAY, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "EndGateway")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertNull(entity.getExitLocation());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "Age", "ExactTeleport"), saved.keySet());
        assertEquals(0L, saved.getLong("Age"));
        assertEquals(NbtType.LONG, NbtType.byClass(saved.get("Age").getClass()));

        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "Age", "ExitPortal"), client.keySet());
        assertEquals(List.of(0, 0, 0), client.getList("ExitPortal", NbtType.INT));
    }
}
