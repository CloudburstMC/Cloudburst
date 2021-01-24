package org.cloudburstmc.server.world.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.server.world.chunk.BlockStorage;
import org.cloudburstmc.server.world.chunk.ChunkBuilder;

interface ChunkSectionSerializer {

    void serialize(ByteBuf buf, BlockStorage[] storage);

    BlockStorage[] deserialize(ByteBuf buf, ChunkBuilder builder);
}
