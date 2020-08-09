package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ChunkSectionSerializerV1 implements ChunkSectionSerializer {

    static final ChunkSectionSerializer INSTANCE = new ChunkSectionSerializerV1();

    @Override
    public void serialize(ByteBuf buf, BlockStorage[] storage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockStorage[] deserialize(ByteBuf buf, ChunkBuilder builder) {
        BlockStorage[] storage = new BlockStorage[2];
        storage[0] = new BlockStorage();
        storage[0].readFromStorage(buf);
        return storage;
    }
}
