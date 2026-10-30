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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void beaconClearedEffectIsNotWritten() throws Exception {
        BeaconBlockEntity entity = (BeaconBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BEACON, POSITION);
        entity.loadAdditionalData(beaconTag());

        // The setters themselves broadcast, which needs a live level; replicate their
        // local state change (they delegate to the DTO) so the save path can be tested standalone.
        ((BeaconData) entity.data()).setPrimary(null);
        ((BeaconData) entity.data()).setSecondary(null);

        assertNull(entity.getPrimaryEffect());
        assertNull(entity.getSecondaryEffect());

        NbtMap saved = entity.getServerTag();
        assertFalse(saved.containsKey("primary"), "server tag still has primary");
        assertFalse(saved.containsKey("secondary"), "server tag still has secondary");

        NbtMap client = entity.getClientTag();
        assertFalse(client.containsKey("primary"), "client tag still has primary");
        assertFalse(client.containsKey("secondary"), "client tag still has secondary");
    }

    @Test
    void beaconBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(BeaconBlockEntity.class, BeaconData.class);
        // The old shape kept the effects in runtime fields named primaryEffect and
        // secondaryEffect, which match no DTO property, and EffectType is not a
        // flagged duplicate-state type, so the shared check stays blind to them.
        // Forbid them explicitly; this fails on the pre-migration class.
        Set<String> fields = new HashSet<>();
        for (Field field : BeaconBlockEntity.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        assertFalse(fields.contains("primaryEffect"),
                "BeaconBlockEntity keeps primaryEffect; state must live in BeaconData only");
        assertFalse(fields.contains("secondaryEffect"),
                "BeaconBlockEntity keeps secondaryEffect; state must live in BeaconData only");
    }

    @Test
    void beaconEffectRoundTrip() {
        BeaconBlockEntity entity = (BeaconBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BEACON, POSITION);
        entity.loadAdditionalData(beaconTag());

        assertEquals(EffectTypes.SPEED, entity.getPrimaryEffect());
        assertEquals(EffectTypes.REGENERATION, entity.getSecondaryEffect());

        NbtMap saved = entity.getServerTag();
        assertEquals((int) NetworkUtils.effectToNetwork(EffectTypes.SPEED), saved.getInt("primary"));
        assertEquals(NbtType.INT, NbtType.byClass(saved.get("primary").getClass()));
        assertEquals((int) NetworkUtils.effectToNetwork(EffectTypes.REGENERATION), saved.getInt("secondary"));
        assertEquals(NbtType.INT, NbtType.byClass(saved.get("secondary").getClass()));

        NbtMap client = entity.getClientTag();
        assertFalse(client.containsKey("primary"), "client tag still has primary");
        assertFalse(client.containsKey("secondary"), "client tag still has secondary");
    }

    @Test
    void beaconUnknownEffectIdLoadFails() {
        // An id the mapping does not know fails the load, exactly like the previous
        // load path, which converted the stored int after reading it.
        BeaconBlockEntity entity = (BeaconBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BEACON, POSITION);
        NbtMap tag = NbtMap.builder()
                .putString("id", "Beacon")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putInt("primary", 99)
                .build();

        Exception failure = assertThrows(Exception.class, () -> entity.loadAdditionalData(tag));
        // The failure is the mapping's own unknown-id error, as on the previous load path.
        Throwable cause = failure;
        boolean recognized = false;
        while (cause != null) {
            if (String.valueOf(cause.getMessage()).contains("Unknown network effect ID: 99")) {
                recognized = true;
                break;
            }
            cause = cause.getCause();
        }
        assertTrue(recognized, "expected an unknown effect id failure, got " + failure);
    }
}
