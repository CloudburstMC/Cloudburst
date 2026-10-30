package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.BedData;
import org.cloudburstmc.api.util.data.DyeColor;
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
 * Round-trip tests for the bed data path: every key today's load/saveClientData
 * handle, plus an unknown key, survive with the same NBT types.
 */
class BedPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap bedTag() {
        return NbtMap.builder()
                .putString("id", "Bed")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Bed")
                .putByte("color", (byte) DyeColor.YELLOW.getWoolData())
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(BedData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.BED));

        BedBlockEntity entity = (BedBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BED, POSITION);
        NbtMap input = bedTag();
        entity.loadAdditionalData(input);

        assertEquals(DyeColor.YELLOW, entity.getColor());

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
        assertEquals(Set.of("id", "color"), client.keySet());
        assertEquals(input.get("color"), client.get("color"), "client value of color");
        assertEquals(NbtType.byClass(input.get("color").getClass()),
                NbtType.byClass(client.get("color").getClass()), "client type of color");
    }

    @Test
    void defaultBedWritesWhiteColor() {
        BedBlockEntity entity = (BedBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.BED, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Bed")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertEquals(DyeColor.WHITE, entity.getColor());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "color"), saved.keySet());
        assertEquals((byte) DyeColor.WHITE.getWoolData(), saved.getByte("color"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("color").getClass()));

        assertEquals(Set.of("id", "color"), entity.getClientTag().keySet());
    }
}
