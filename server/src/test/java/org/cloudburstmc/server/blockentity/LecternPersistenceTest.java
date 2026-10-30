package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.data.LecternData;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the lectern data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 */
class LecternPersistenceTest {

    private static final Vector3i POSITION = Vector3i.from(10, 64, -5);

    private static NbtMap lecternTag() {
        return NbtMap.builder()
                .putString("id", "Lectern")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("isMovable", true)
                .putString("CustomName", "Lectern")
                .putBoolean("hasBook", true)
                .putCompound("book", ItemUtils.serializeItem(ItemStack.from(ItemTypes.WRITTEN_BOOK, 1)))
                .putInt("page", 2)
                .putInt("totalPages", 5)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(LecternData.class,
                CloudBlockEntityRegistry.get().getDataClass(BlockEntityTypes.LECTERN));

        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        NbtMap input = lecternTag();
        entity.loadAdditionalData(input);

        assertTrue(entity.hasBook());
        assertEquals(ItemStack.from(ItemTypes.WRITTEN_BOOK, 1), entity.getBook());
        assertEquals(2, entity.getPage());
        assertEquals(5, entity.getTotalPages());

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
    void lecternWithoutBookWritesNoBookKeys() {
        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Lectern")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertFalse(entity.hasBook());
        assertTrue(entity.getBook().isEmpty());
        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
        assertEquals(Set.of("id"), entity.getClientTag().keySet());
    }

    @Test
    void lecternTakenBookIsNotWritten() throws Exception {
        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        entity.loadAdditionalData(lecternTag());

        // setBook() itself updates redstone, which needs a live level; replicate its
        // local state change (taking the book clears it) so the save path can be tested standalone.
        Field book = LecternBlockEntity.class.getDeclaredField("book");
        book.setAccessible(true);
        book.set(entity, null);

        assertFalse(entity.hasBook());

        NbtMap saved = entity.getServerTag();
        for (String key : Set.of("hasBook", "book", "page", "totalPages")) {
            assertFalse(saved.containsKey(key), "server tag still has " + key);
        }

        NbtMap client = entity.getClientTag();
        for (String key : Set.of("hasBook", "book", "page", "totalPages")) {
            assertFalse(client.containsKey(key), "client tag still has " + key);
        }
    }

    @Test
    void explicitFalseHasBookIsNotWritten() {
        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Lectern")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("hasBook", false)
                .build());

        assertFalse(entity.hasBook());

        // An empty lectern stores no book keys; a loaded hasBook:0b stays dropped, exactly as before.
        assertFalse(entity.getServerTag().containsKey("hasBook"), "server tag gained hasBook");
        assertFalse(entity.getClientTag().containsKey("hasBook"), "client tag gained hasBook");
    }
}
