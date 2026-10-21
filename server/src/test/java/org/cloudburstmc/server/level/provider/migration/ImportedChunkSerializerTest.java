package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.level.provider.leveldb.serializer.ImportedChunkSerializer;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.iq80.leveldb.WriteBatch;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ImportedChunkSerializerTest {

    @Test
    void writesEntityRecordsAndPendingTicksWithoutCreatingLiveEntities() throws Exception {
        Level level = InterfaceProxy.create(Level.class, Map.of("getSectionsCount", 24));
        CloudChunk chunk = new CloudChunk(-2, 3, level);
        NbtMap entity = NbtMap.builder().putString("identifier", "minecraft:pig").build();
        NbtMap blockEntity = NbtMap.builder().putString("id", "Chest").putInt("x", -31).build();
        NbtMap ticks = NbtMap.builder().putInt("currentTick", 0)
                .putList("tickList", NbtType.COMPOUND, List.of(NbtMap.builder().putLong("time", 37).build())).build();
        ImportedChunk imported = new ImportedChunk(chunk, List.of(entity), List.of(blockEntity), ticks);
        Map<String, byte[]> records = new HashMap<>();
        WriteBatch batch = InterfaceProxy.create(WriteBatch.class, (proxy, method, arguments) -> {
            if (method.getName().equals("put")) {
                records.put(HexFormat.of().formatHex((byte[]) arguments[0]), (byte[]) arguments[1]);
                return proxy;
            }

            throw new AssertionError("Unexpected batch operation: " + method);
        });

        ImportedChunkSerializer.serialize(batch, -2, 3, imported);

        assertEquals(3, records.size());
        assertEquals(entity, read(records, LevelDBKey.ENTITIES));
        assertEquals(blockEntity, read(records, LevelDBKey.BLOCK_ENTITIES));
        assertEquals(ticks, read(records, LevelDBKey.PENDING_TICKS));
        assertTrue(chunk.getEntities().isEmpty());
    }

    private static NbtMap read(Map<String, byte[]> records, LevelDBKey key) throws Exception {
        byte[] data = records.get(HexFormat.of().formatHex(key.getKey(-2, 3)));
        assertNotNull(data);
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(data);
             NBTInputStream input = NbtUtils.createReaderLE(bytes)) {
            NbtMap tag = assertInstanceOf(NbtMap.class, input.readTag());
            assertEquals(0, bytes.available());
            return tag;
        }
    }
}
