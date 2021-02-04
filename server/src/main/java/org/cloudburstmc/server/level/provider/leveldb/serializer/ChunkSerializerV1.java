package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.utils.ChunkException;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ChunkSerializerV1 implements ChunkSerializer {

    static final ChunkSerializer INSTANCE = new ChunkSerializerV1();

    @Override
    public void serialize(WriteBatch db, CloudChunk chunk) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deserialize(DB db, ChunkBuilder chunkBuilder) {
        this.deserializeExtraData(db, chunkBuilder);

        this.deserializeTerrain(db, chunkBuilder);
    }

    protected void deserializeTerrain(DB db, ChunkBuilder chunkBuilder) {
        byte[] terrain = db.get(LevelDBKey.LEGACY_TERRAIN.getKey(chunkBuilder.getX(), chunkBuilder.getZ()));
        if (terrain == null) {
            throw new ChunkException("No terrain found in chunk");
        }
    }

    protected void deserializeExtraData(DB db, ChunkBuilder chunkBuilder) {
        byte[] extraData = db.get(LevelDBKey.BLOCK_EXTRA_DATA.getKey(chunkBuilder.getX(), chunkBuilder.getZ()));
        if (extraData == null) {
            return;
        }
        ByteBuf buf = Unpooled.wrappedBuffer(extraData);

        int count = buf.readIntLE();
        for (int i = 0; i < count; i++) {
            int key = deserializeExtraDataKey(buf.readIntLE());
            short value = buf.readShortLE();

            chunkBuilder.extraData(key, value);
        }
    }

    protected int deserializeExtraDataKey(int key) {
        return ((key & ~0x7f) << 1) | (key & 0x7f); // max world height was only 128
    }
}
