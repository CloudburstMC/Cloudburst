package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.BeaconData;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Round-trip tests for the beacon data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class BeaconPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap beaconTag() {
        return NbtMap.builder()
                .putString("id", "Beacon")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Beacon")
                .putInt("primary", NetworkUtils.effectToNetwork(EffectTypes.SPEED))
                .putInt("secondary", NetworkUtils.effectToNetwork(EffectTypes.REGENERATION))
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(BeaconData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.BEACON));

        BeaconBlockEntity entity = (BeaconBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BEACON, POSITION);
        NbtMap input = beaconTag();
        entity.loadAdditionalData(input);

        assertEquals(EffectTypes.SPEED, entity.getPrimaryEffect());
        assertEquals(EffectTypes.REGENERATION, entity.getSecondaryEffect());

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void effectlessBeaconOmitsEffectKeys() {
        BeaconBlockEntity entity = (BeaconBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BEACON, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Beacon")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertNull(entity.getPrimaryEffect());
        assertNull(entity.getSecondaryEffect());

        // Today's save throws on null effects; the data path omits the keys instead
        // so a configured beacon is never required before saving.
        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }
}
