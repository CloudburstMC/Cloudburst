package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the piston data path. Today's save writes none of the piston
 * keys; they survive through the raw tag, which the data path preserves untouched.
 */
class PistonPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap pistonTag() {
        return NbtMap.builder()
                .putString("id", "PistonArm")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Piston")
                .putFloat("Progress", 0.5f)
                .putFloat("LastProgress", 0.25f)
                .putByte("State", (byte) 0)
                .putByte("NewState", (byte) 2)
                .putBoolean("Sticky", true)
                .putList("AttachedBlocks", NbtType.INT, List.of(1, 2, 3, 4, 5, 6))
                .putList("BreakBlocks", NbtType.INT, List.of(7, 8, 9))
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(BlockEntityData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.PISTON));

        PistonBlockEntity entity = (PistonBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.PISTON, POSITION);
        NbtMap input = pistonTag();
        entity.loadAdditionalData(input);

        assertEquals(0.5f, entity.getProgress());
        assertEquals(0.25f, entity.getLastProgress());
        assertTrue(entity.isSticky());

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
    void defaultPistonAddsNoKeys() {
        PistonBlockEntity entity = (PistonBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.PISTON, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "PistonArm")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertEquals(1.0f, entity.getProgress());
        assertEquals(1.0f, entity.getLastProgress());

        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }
}
