package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.ldbjni.direct.DirectWriteBatch;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;

public class ChunkSerializers {

    private static final IntObjectMap<ChunkSerializer> SERIALIZERS = new IntObjectHashMap<>();

    static {
        SERIALIZERS.put(0, ChunkSerializerV1.INSTANCE);
        SERIALIZERS.put(1, ChunkSerializerV1.INSTANCE);
        SERIALIZERS.put(2, ChunkSerializerV1.INSTANCE);
        SERIALIZERS.put(3, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(4, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(6, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(7, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(9, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(10, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(11, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(12, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(13, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(14, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(15, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(16, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(17, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(18, ChunkSerializerV3.INSTANCE);
        SERIALIZERS.put(19, ChunkSerializerV3.INSTANCE);
    }

    private static ChunkSerializer getChunkSerializer(int version) {
        ChunkSerializer chunkSerializer = SERIALIZERS.get(version);
        if (chunkSerializer == null) {
            throw new IllegalArgumentException("Invalid chunk serialize version " + version);
        }
        return chunkSerializer;
    }

    public static void serializeChunk(DirectWriteBatch db, Chunk chunk, int version) {
        getChunkSerializer(version).serialize(db, chunk);
    }

    public static void deserializeChunk(DirectDB db, ChunkBuilder chunkBuilder, int version) {
        getChunkSerializer(version).deserialize(db, chunkBuilder);
    }
}
