package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ChunkSectionSerializerV1 implements ChunkSectionSerializer {

    static final ChunkSectionSerializer INSTANCE = new ChunkSectionSerializerV1();

    @Override
    public void serialize(ByteBuf buf, BlockStorage[] storage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockStorage[] deserialize(ByteBuf buf, CloudChunkBuilder builder) {
        return new BlockStorage[]{BlockStorage.readFromStorage(buf), new BlockStorage()};
    }
}
