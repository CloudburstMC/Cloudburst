package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;

public class ChunkSectionSerializers {

    public static void serialize(ByteBuf buf, BlockStorage[] storage, int version) {
        getSerializer(version).serialize(buf, storage);
    }

    public static BlockStorage[] deserialize(ByteBuf buf, CloudChunkBuilder builder, int version) {
        return getSerializer(version).deserialize(buf, builder);
    }

    public static ChunkSectionSerializer getSerializer(int version) {
        return switch (version) {
            case 1 -> ChunkSectionSerializerV1.INSTANCE;
            case 8 -> ChunkSectionSerializerV8.INSTANCE;
            case 9 -> ChunkSectionSerializerV9.INSTANCE;
            case 0, 2, 3, 4, 5, 6, 7 -> throw new UnsupportedOperationException("Numeric-ID sub-chunk storage is no longer supported: version " + version);
            default -> throw new IllegalArgumentException("Invalid sub-chunk version " + version);
        };
    }
}
