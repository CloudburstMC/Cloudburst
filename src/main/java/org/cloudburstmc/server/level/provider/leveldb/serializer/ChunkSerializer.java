package org.cloudburstmc.server.level.provider.leveldb.serializer;

import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.ldbjni.direct.DirectWriteBatch;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;

interface ChunkSerializer {

    void serialize(DirectWriteBatch db, Chunk chunk);

    void deserialize(DirectDB db, ChunkBuilder chunkBuilder);
}
