package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;

interface ChunkSectionSerializer {

    void serialize(ByteBuf buf, BlockStorage[] storage);

    BlockStorage[] deserialize(ByteBuf buf, ChunkBuilder builder);
}
