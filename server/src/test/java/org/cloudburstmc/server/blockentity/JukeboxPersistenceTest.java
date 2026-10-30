package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.JukeboxData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the jukebox data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class JukeboxPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap jukeboxTag() {
        return NbtMap.builder()
                .putString("id", "Jukebox")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Jukebox")
                .putCompound("RecordItem", ItemUtils.serializeItem(ItemStack.from(ItemTypes.MUSIC_DISC_13, 1)))
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(JukeboxData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.JUKEBOX));

        JukeboxBlockEntity entity = (JukeboxBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.JUKEBOX, POSITION);
        NbtMap input = jukeboxTag();
        entity.loadAdditionalData(input);

        assertEquals(ItemStack.from(ItemTypes.MUSIC_DISC_13, 1), entity.getRecordItem());

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
    void emptyJukeboxOmitsRecordItem() {
        JukeboxBlockEntity entity = (JukeboxBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.JUKEBOX, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Jukebox")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertTrue(entity.getRecordItem().isEmpty());
        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void airRecordItemStaysOmitted() {
        JukeboxBlockEntity entity = (JukeboxBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.JUKEBOX, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Jukebox")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putCompound("RecordItem", ItemUtils.serializeItem(ItemStack.EMPTY))
                .build());

        assertTrue(entity.getRecordItem().isEmpty());
        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
    }
}
