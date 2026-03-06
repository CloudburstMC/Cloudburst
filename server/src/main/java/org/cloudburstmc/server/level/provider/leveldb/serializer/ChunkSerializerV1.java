package org.cloudburstmc.server.level.provider.leveldb.serializer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.ldbjni.direct.DirectWriteBatch;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.chunk.ChunkException;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.iq80.leveldb.DB;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ChunkSerializerV1 implements ChunkSerializer {

    static final ChunkSerializer INSTANCE = new ChunkSerializerV1();

    @Override
    public void serialize(DirectWriteBatch db, Chunk chunk) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deserialize(DirectDB db, ChunkBuilder chunkBuilder) {
        this.deserializeTerrain(db, chunkBuilder);
    }

    protected void deserializeTerrain(DB db, ChunkBuilder chunkBuilder) {
        byte[] terrain = db.get(LevelDBKey.LEGACY_TERRAIN.getKey(chunkBuilder.getX(), chunkBuilder.getZ()));
        if (terrain == null) {
            throw new ChunkException("No terrain found in chunk");
        }
    }
}
