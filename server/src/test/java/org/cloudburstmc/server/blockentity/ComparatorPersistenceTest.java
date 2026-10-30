package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.ComparatorData;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Round-trip tests for the comparator data path: every key today's load/save
 * handle, plus an unknown key, survive with the same NBT types.
 */
class ComparatorPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap comparatorTag() {
        return NbtMap.builder()
                .putString("id", "Comparator")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Comparator")
                .putInt("OutputSignal", 9)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(ComparatorData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.COMPARATOR));

        ComparatorBlockEntity entity = (ComparatorBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.COMPARATOR, POSITION);
        NbtMap input = comparatorTag();
        entity.loadAdditionalData(input);

        assertEquals(9, entity.getOutputSignal());

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
    void defaultComparatorWritesZeroSignal() {
        ComparatorBlockEntity entity = (ComparatorBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.COMPARATOR, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Comparator")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertEquals(0, entity.getOutputSignal());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "OutputSignal"), saved.keySet());
        assertEquals(0, saved.getInt("OutputSignal"));
        assertEquals(NbtType.INT, NbtType.byClass(saved.get("OutputSignal").getClass()));

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }
}
