package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.iq80.leveldb.DB;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LevelDBEntityReaderTest {

    @Test
    void readsIndexedActorsAlongsideDistinctChunkLocalEntities() throws IOException {
        Records records = new Records();
        NbtMap legacy = NbtMap.builder().putLong("UniqueID", 1).putString("identifier", "minecraft:pig").build();
        NbtMap actor = NbtMap.builder().putLong("UniqueID", -8589934590L).putString("identifier", "minecraft:boat").build();
        byte[] storageKey = HexFormat.of().parseHex("0000000200000002");
        NbtMap chest = NbtMap.builder().putString("id", "Chest").build();
        records.put(LevelDBImportKeys.key(-3, 7, 1, '2'), encoded(legacy));
        records.put(LevelDBImportKeys.key(-3, 7, 1, '1'), encoded(chest));
        records.put(LevelDBImportKeys.actorIndex(-3, 7, 1), storageKey);
        records.put(LevelDBImportKeys.actor(storageKey), encoded(actor));

        assertEquals(List.of(legacy, actor), records.reader(1).readEntities(-3, 7));
        assertEquals(List.of(chest), records.reader(1).readBlockEntities(-3, 7));
        assertTrue(records.reader(0).readEntities(-3, 7).isEmpty());
        assertTrue(records.reader(2).readBlockEntities(-3, 7).isEmpty());
    }

    @Test
    void missingInvalidAndDuplicateActorReferencesFailInsteadOfDroppingEntities() throws IOException {
        Records records = new Records();
        byte[] storageKey = HexFormat.of().parseHex("0000000200000002");
        records.put(LevelDBImportKeys.actorIndex(0, 0, 0), storageKey);
        LevelDBEntityReader reader = records.reader(0);

        assertThrows(IOException.class, () -> reader.readEntities(0, 0));

        records.put(LevelDBImportKeys.actor(storageKey), encoded(NbtMap.builder().putInt("UniqueID", 42).build()));
        assertThrows(IOException.class, () -> reader.readEntities(0, 0));

        records.put(LevelDBImportKeys.actor(storageKey), encoded(NbtMap.builder().putLong("UniqueID", 42).build()));
        records.put(LevelDBImportKeys.actorIndex(0, 0, 0), keys(storageKey, storageKey));
        assertThrows(IOException.class, () -> reader.readEntities(0, 0));

        records.put(LevelDBImportKeys.actorIndex(0, 0, 0), new byte[7]);
        assertThrows(IOException.class, () -> reader.readEntities(0, 0));
    }

    @Test
    void rejectsDuplicateEntityIdsAcrossDistinctStorageKeysAndChunkLocalEntities() throws IOException {
        Records records = new Records();
        byte[] first = HexFormat.of().parseHex("0000000200000002");
        byte[] second = HexFormat.of().parseHex("0000000200000003");
        NbtMap actor = NbtMap.builder().putLong("UniqueID", -8589934590L).build();
        records.put(LevelDBImportKeys.actor(first), encoded(actor));
        records.put(LevelDBImportKeys.actor(second), encoded(actor));
        records.put(LevelDBImportKeys.actorIndex(0, 0, 0), keys(first, second));

        assertThrows(IOException.class, () -> records.reader(0).readEntities(0, 0));

        records.put(LevelDBImportKeys.actorIndex(0, 0, 0), first);
        records.put(LevelDBImportKeys.key(0, 0, 0, '2'), encoded(actor));

        assertThrows(IOException.class, () -> records.reader(0).readEntities(0, 0));
    }

    private static byte[] encoded(NbtMap tag) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTOutputStream output = NbtUtils.createWriterLE(bytes)) {
            output.writeTag(tag);
        }

        return bytes.toByteArray();
    }

    private static byte[] keys(byte[]... keys) {
        ByteBuffer data = ByteBuffer.allocate(keys.length * LevelDBImportKeys.ACTOR_STORAGE_KEY_SIZE);
        for (byte[] key : keys) {
            data.put(key);
        }

        return data.array();
    }

    private static class Records {

        private final Map<ByteBuffer, byte[]> data = new HashMap<>();

        private void put(byte[] key, byte[] value) {
            this.data.put(ByteBuffer.wrap(key), value);
        }

        private LevelDBEntityReader reader(int dimension) {
            DB database = InterfaceProxy.create(DB.class, (proxy, method, arguments) -> {
                if (method.getName().equals("get")) {
                    return this.data.get(ByteBuffer.wrap((byte[]) arguments[0]));
                }

                throw new AssertionError("Unexpected database access: " + method);
            });

            return new LevelDBEntityReader(database, dimension);
        }
    }
}
