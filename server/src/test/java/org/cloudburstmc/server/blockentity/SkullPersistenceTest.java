package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.SkullData;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the skull data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class SkullPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap skullTag() {
        return NbtMap.builder()
                .putString("id", "Skull")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Skull")
                .putByte("SkullType", (byte) 1)
                .putFloat("Rotation", 45.5f)
                .putInt("MouthTickCount", 3)
                .putBoolean("MouthMoving", true)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(SkullData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.SKULL));

        SkullBlockEntity entity = (SkullBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SKULL, POSITION);
        NbtMap input = skullTag();
        entity.loadAdditionalData(input);

        assertEquals(1, entity.getSkullType());
        assertEquals(45.5f, entity.getRotation());
        assertEquals(3, entity.getMouthTickCount());
        assertTrue(entity.isMouthMoving());

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
    void defaultSkullWritesZeroedKeys() {
        SkullBlockEntity entity = (SkullBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.SKULL, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Skull")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "SkullType", "Rotation", "MouthTickCount", "MouthMoving"),
                saved.keySet());
        assertEquals((byte) 0, saved.getByte("SkullType"));
        assertEquals(0.0f, saved.getFloat("Rotation"));

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void skullBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(SkullBlockEntity.class, SkullData.class);
    }
}
