package org.cloudburstmc.server.world.provider.leveldb.serializer;

import org.cloudburstmc.server.world.chunk.Chunk;
import org.cloudburstmc.server.world.chunk.ChunkBuilder;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;

interface ChunkSerializer {

    void serialize(WriteBatch db, Chunk chunk);

    void deserialize(DB db, ChunkBuilder chunkBuilder);
}
