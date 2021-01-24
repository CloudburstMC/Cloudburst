package org.cloudburstmc.server.world.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.world.chunk.BlockStorage;
import org.cloudburstmc.server.world.chunk.ChunkBuilder;
import org.cloudburstmc.server.world.provider.leveldb.BlockStorageConverter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ChunkSectionSerializerV7 implements ChunkSectionSerializer {
    static final ChunkSectionSerializer INSTANCE = new ChunkSectionSerializerV7();

    @Override
    public void serialize(ByteBuf buf, BlockStorage[] storage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockStorage[] deserialize(ByteBuf buf, ChunkBuilder builder) {
        byte[] blockIds = new byte[4096];
        buf.readBytes(blockIds);
        byte[] blockData = new byte[2048];
        buf.readBytes(blockData);
        if (buf.isReadable(4096)) {
            buf.skipBytes(4096); // light
            builder.dirty();
        }

        BlockStorage[] blockStorage = new BlockStorage[2];
        blockStorage[0] = BlockStorageConverter.fromXZY(blockIds, blockData);
        return blockStorage;
    }
}
