package org.cloudburstmc.server.level.provider.leveldb.serializer;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.iq80.leveldb.WriteBatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Writes converted entity records into the same batch as imported terrain.
 */
@UtilityClass
public class ImportedChunkSerializer {

    public static void serialize(WriteBatch batch, int x, int z, ImportedChunk imported) throws IOException {
        write(batch, LevelDBKey.ENTITIES.getKey(x, z), imported.entities());
        write(batch, LevelDBKey.BLOCK_ENTITIES.getKey(x, z), imported.blockEntities());
        write(batch, LevelDBKey.PENDING_TICKS.getKey(x, z), imported.pendingTicks().getList("tickList", NbtType.COMPOUND).isEmpty() ? List.of() : List.of(imported.pendingTicks()));
    }

    private static void write(WriteBatch batch, byte[] key, List<NbtMap> records) throws IOException {
        if (records.isEmpty()) {
            batch.delete(key);
            return;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTOutputStream output = NbtUtils.createWriterLE(bytes)) {
            for (NbtMap record : records) {
                output.writeTag(record);
            }
        }

        batch.put(key, bytes.toByteArray());
    }
}
