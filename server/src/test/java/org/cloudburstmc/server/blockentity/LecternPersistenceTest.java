package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.data.CloudLecternData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
                .putCompound("book", ItemStackNbtCodec.serializeItem(ItemStack.from(ItemTypes.WRITTEN_BOOK, 1)))
                .putInt("page", 2)
                .putInt("totalPages", 5)
                .putInt("Mystery", 7)
                .build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {

        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        NbtMap input = lecternTag();
        entity.loadAdditionalData(input);

        assertTrue(entity.hasBook());
        assertEquals(ItemStack.from(ItemTypes.WRITTEN_BOOK, 1), entity.getBook());
        assertEquals(0, entity.getPage());
        assertEquals(0, entity.getTotalPages());

        NbtMap saved = entity.getServerTag();
        Set<String> expectedKeys = new HashSet<>(input.keySet());
        expectedKeys.addAll(Set.of("id", "x", "y", "z"));
        assertEquals(expectedKeys, saved.keySet());
        for (String key : input.keySet()) {
            if (key.equals("page") || key.equals("totalPages")) {
                assertEquals(0, saved.getInt(key));
                continue;
            }
            assertEquals(input.get(key), saved.get(key), "value of " + key);
            assertEquals(NbtType.byClass(input.get(key).getClass()),
                    NbtType.byClass(saved.get(key).getClass()), "type of " + key);
        }

        Set<String> clientKeys = Set.of(
                "id", "x", "y", "z", "BlockEntityVersion", "hasBook", "book", "page", "totalPages"
        );
        assertEquals(clientKeys, entity.getClientTag().keySet());
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
        assertEmptyClientTag(entity);
    }

    @Test
    void lecternTakenBookIsNotWritten() {
        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        entity.loadAdditionalData(lecternTag());

        CloudLecternData data = (CloudLecternData) entity.data();
        data.setBook(null);
        data.setHasBook(null);
        data.setPage(null);
        data.setTotalPages(null);

        assertFalse(entity.hasBook());

        NbtMap saved = entity.getServerTag();
        for (String key : Set.of("hasBook", "book", "page", "totalPages")) {
            assertFalse(saved.containsKey(key), "server tag still has " + key);
        }

        assertEmptyClientTag(entity);
    }

    @Test
    void setPageOnEmptyLecternWritesNoPageKey() {
        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Lectern")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertFalse(entity.hasBook());

        entity.setPage(3);

        assertFalse(entity.hasBook());
        assertEquals(0, entity.getPage());
        assertEquals(0, entity.getTotalPages());

        NbtMap saved = entity.getServerTag();
        assertFalse(saved.containsKey("page"), "server tag gained page");
        assertFalse(saved.containsKey("totalPages"), "server tag gained totalPages");

        NbtMap client = entity.getClientTag();
        assertFalse(client.containsKey("page"), "client tag gained page");
        assertFalse(client.containsKey("totalPages"), "client tag gained totalPages");
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

        assertFalse(entity.getServerTag().containsKey("hasBook"), "server tag gained hasBook");
        assertFalse(entity.getClientTag().getBoolean("hasBook"));
    }

    @Test
    void absentKeysKeepTodayDefaults() {
        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Lectern")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .build());

        assertFalse(entity.hasBook());
        assertTrue(entity.getBook().isEmpty());
        assertEquals(0, entity.getPage());
        assertEquals(0, entity.getTotalPages());
        assertEquals(Set.of("id", "x", "y", "z", "isMovable"), entity.getServerTag().keySet());
        assertEmptyClientTag(entity);
    }

    @Test
    void bookWithoutHasBookIsIgnored() {
        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Lectern")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putCompound("book", ItemStackNbtCodec.serializeItem(ItemStack.from(ItemTypes.WRITTEN_BOOK, 1)))
                .putInt("page", 2)
                .putInt("totalPages", 5)
                .build());

        assertFalse(entity.hasBook());
        assertTrue(entity.getBook().isEmpty());
        assertEquals(0, entity.getPage());
        assertEquals(0, entity.getTotalPages());

        NbtMap saved = entity.getServerTag();
        for (String key : Set.of("hasBook", "book", "page", "totalPages")) {
            assertFalse(saved.containsKey(key), "server tag gained " + key);
        }
    }

    @Test
    void bookWithMissingPageKeysLoadsZero() {
        LecternBlockEntity entity = (LecternBlockEntity) PersistenceFixtures.blockEntity(BlockEntityTypes.LECTERN, POSITION);
        entity.loadAdditionalData(NbtMap.builder()
                .putString("id", "Lectern")
                .putInt("x", 10)
                .putInt("y", 64)
                .putInt("z", -5)
                .putBoolean("hasBook", true)
                .putCompound("book", ItemStackNbtCodec.serializeItem(ItemStack.from(ItemTypes.WRITTEN_BOOK, 1)))
                .build());

        assertTrue(entity.hasBook());
        assertEquals(0, entity.getPage());
        assertEquals(0, entity.getTotalPages());

        NbtMap saved = entity.getServerTag();
        assertTrue(saved.containsKey("page"), "server tag lost page");
        assertEquals(0, saved.getInt("page"));
        assertEquals(NbtType.INT, NbtType.byClass(saved.get("page").getClass()), "type of page");
        assertTrue(saved.containsKey("totalPages"), "server tag lost totalPages");
        assertEquals(0, saved.getInt("totalPages"));
        assertEquals(NbtType.INT, NbtType.byClass(saved.get("totalPages").getClass()), "type of totalPages");
    }

    private static void assertEmptyClientTag(LecternBlockEntity entity) {
        NbtMap client = entity.getClientTag();
        assertEquals(Set.of("id", "x", "y", "z", "BlockEntityVersion", "hasBook"), client.keySet());
        assertFalse(client.getBoolean("hasBook"));
    }

    @Test
    void lecternBlockEntityKeepsNoDuplicateState() {
        DtoStateChecks.assertKeepsNoDuplicateState(LecternBlockEntity.class, CloudLecternData.class);
    }
}
