package org.cloudburstmc.server.level.provider.migration;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.iq80.leveldb.DB;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Reads chunk-local entities and indexed actors without instantiating them.
 */
public class LevelDBEntityReader {

    private final DB database;
    private final int dimension;

    public LevelDBEntityReader(DB database, int dimension) {
        this.database = Objects.requireNonNull(database, "database");
        this.dimension = dimension;
    }

    /**
     * Reads entities referenced by a source chunk, including indexed actors.
     * Saved positions may belong to other chunks. Missing actors, repeated storage keys and repeated
     * long UniqueID values within this chunk's records fail.
     */
    public List<NbtMap> readEntities(int x, int z) throws IOException {
        List<NbtMap> entities = new ArrayList<>(compounds(this.database.get(LevelDBImportKeys.key(x, z, this.dimension, '2'))));
        Set<Long> uniqueIds = new HashSet<>();
        for (NbtMap entity : entities) {
            if (entity.containsKey("UniqueID", NbtType.LONG) && !uniqueIds.add(entity.getLong("UniqueID"))) {
                throw new IOException("Duplicate imported actor ID: " + entity.getLong("UniqueID"));
            }
        }

        byte[] index = this.database.get(LevelDBImportKeys.actorIndex(x, z, this.dimension));
        if (index == null) {
            return entities;
        }

        if (index.length % LevelDBImportKeys.ACTOR_STORAGE_KEY_SIZE != 0) {
            throw new IOException("Invalid LevelDB actor index length");
        }

        Set<String> storageKeys = new HashSet<>();
        for (int offset = 0; offset < index.length; offset += LevelDBImportKeys.ACTOR_STORAGE_KEY_SIZE) {
            byte[] storageKey = Arrays.copyOfRange(index, offset, offset + LevelDBImportKeys.ACTOR_STORAGE_KEY_SIZE);
            String key = HexFormat.of().formatHex(storageKey);
            if (!storageKeys.add(key)) {
                throw new IOException("Duplicate imported actor storage key: " + key);
            }

            List<NbtMap> records = compounds(this.database.get(LevelDBImportKeys.actor(storageKey)));
            if (records.size() != 1 || !records.getFirst().containsKey("UniqueID", NbtType.LONG)) {
                throw new IOException("Missing or invalid imported actor at storage key: " + key);
            }

            NbtMap actor = records.getFirst();
            if (!uniqueIds.add(actor.getLong("UniqueID"))) {
                throw new IOException("Duplicate imported actor ID: " + actor.getLong("UniqueID"));
            }

            entities.add(actor);
        }

        return entities;
    }

    /**
     * Reads raw block entity compounds for a source chunk, returning an empty list when absent.
     */
    public List<NbtMap> readBlockEntities(int x, int z) throws IOException {
        return compounds(this.database.get(LevelDBImportKeys.key(x, z, this.dimension, '1')));
    }

    private static List<NbtMap> compounds(byte @Nullable [] data) throws IOException {
        if (data == null) {
            return List.of();
        }

        List<NbtMap> tags = new ArrayList<>();
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(data);
             NBTInputStream input = NbtUtils.createReaderLE(bytes)) {
            while (bytes.available() > 0) {
                if (!(input.readTag() instanceof NbtMap tag)) {
                    throw new IOException("Imported entity data must contain compounds");
                }

                tags.add(tag);
            }
        }

        return tags;
    }
}
