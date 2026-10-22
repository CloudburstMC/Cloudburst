package org.cloudburstmc.server.level.provider.leveldb.serializer;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector2i;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.level.provider.migration.ImportedEntityData;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.WriteBatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists converted records and reconciles imported entity chunk ownership.
 */
@UtilityClass
@Log4j2
public class ImportedChunkSerializer {

    /**
     * Adds converted entities, block entities and pending ticks to the caller's terrain batch.
     * The caller retains ownership of the chunk and must commit the batch.
     */
    public static void serialize(WriteBatch batch, int x, int z, ImportedChunk imported) throws IOException {
        write(batch, LevelDBKey.ENTITIES.getKey(x, z), imported.entities());
        write(batch, LevelDBKey.BLOCK_ENTITIES.getKey(x, z), imported.blockEntities());
        write(batch, LevelDBKey.PENDING_TICKS.getKey(x, z), imported.pendingTicks().getList("tickList", NbtType.COMPOUND).isEmpty() ? List.of() : List.of(imported.pendingTicks()));
    }

    /**
     * Reassigns stored entity records to the chunks containing their saved positions.
     * The caller must finish all chunk saves before calling and prevent concurrent writes.
     * Destination chunks must have a current or legacy version record. Changes are committed
     * in one batch after validation and do not change entity positions or block entity records.
     */
    public static void relocateEntities(DB database) throws IOException {
        Map<Vector2i, List<NbtMap>> updates = new HashMap<>();
        Map<Vector2i, List<NbtMap>> arrivals = new HashMap<>();
        int relocated = 0;
        try (DBIterator iterator = database.iterator()) {
            iterator.seekToFirst();
            while (iterator.hasNext()) {
                Map.Entry<byte[], byte[]> entry = iterator.next();
                byte[] key = entry.getKey();
                if (key.length != 9 || key[8] != '2') {
                    continue;
                }

                ByteBuffer coordinates = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
                Vector2i storedChunk = Vector2i.from(coordinates.getInt(), coordinates.getInt());
                List<NbtMap> records = readRecords(entry.getValue());
                List<NbtMap> retained = new ArrayList<>();
                for (NbtMap entity : records) {
                    Vector2i destination = ImportedEntityData.chunkPosition(entity);
                    if (destination.equals(storedChunk)) {
                        retained.add(entity);
                    } else {
                        arrivals.computeIfAbsent(destination, ignored -> new ArrayList<>()).add(entity);
                        relocated++;
                    }
                }

                if (retained.size() != records.size()) {
                    updates.put(storedChunk, retained);
                }
            }
        }

        if (relocated == 0) {
            return;
        }

        for (Map.Entry<Vector2i, List<NbtMap>> entry : arrivals.entrySet()) {
            Vector2i destination = entry.getKey();
            int x = destination.getX();
            int z = destination.getY();
            if (database.get(LevelDBKey.VERSION.getKey(x, z)) == null && database.get(LevelDBKey.VERSION_OLD.getKey(x, z)) == null) {
                throw new IOException("Imported entity targets a chunk without terrain: " + x + "," + z);
            }

            List<NbtMap> records = updates.get(destination);
            if (records == null) {
                records = new ArrayList<>(readRecords(database.get(LevelDBKey.ENTITIES.getKey(x, z))));
                updates.put(destination, records);
            }

            records.addAll(entry.getValue());
        }

        try (WriteBatch batch = database.createWriteBatch()) {
            for (Map.Entry<Vector2i, List<NbtMap>> entry : updates.entrySet()) {
                Vector2i chunk = entry.getKey();
                write(batch, LevelDBKey.ENTITIES.getKey(chunk.getX(), chunk.getY()), entry.getValue());
            }

            database.write(batch);
        }

        log.debug("Corrected imported entity chunk assignments: {}", relocated);
    }

    private static List<NbtMap> readRecords(byte @Nullable [] data) throws IOException {
        if (data == null) {
            return List.of();
        }

        List<NbtMap> records = new ArrayList<>();
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(data);
             NBTInputStream input = NbtUtils.createReaderLE(bytes)) {
            while (bytes.available() > 0) {
                if (!(input.readTag() instanceof NbtMap record)) {
                    throw new IOException("Imported entity records must be compounds");
                }

                records.add(record);
            }
        }

        return records;
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
