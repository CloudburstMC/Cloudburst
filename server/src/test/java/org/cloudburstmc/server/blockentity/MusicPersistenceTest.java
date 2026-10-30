package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.MusicData;
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
 * Round-trip tests for the music (note block) data path: every key today's
 * load/save handle, plus an unknown key, survive with the same NBT types.
 */
class MusicPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap musicTag() {
        return NbtMap.builder()
                .putString("id", "Music")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Music")
                .putByte("note", (byte) 12)
                .putBoolean("powered", true)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(MusicData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.NOTEBLOCK));

        MusicBlockEntity entity = (MusicBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.NOTEBLOCK, POSITION);
        NbtMap input = musicTag();
        entity.loadAdditionalData(input);

        assertEquals(12, entity.getNote());
        assertTrue(entity.isPowered());

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
    void defaultMusicBlockWritesSilentKeys() {
        MusicBlockEntity entity = (MusicBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.NOTEBLOCK, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Music")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        NbtMap saved = entity.getServerTag();
        assertEquals(Set.of("id", "x", "y", "z", "isMovable", "note", "powered"), saved.keySet());
        assertEquals((byte) 0, saved.getByte("note"));
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("note").getClass()));

        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }
}
