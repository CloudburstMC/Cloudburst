package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;

/**
 * Handles on-disk sub-chunk version 9 (PALETTED_MULTI_WITH_OFFSET).
 *
 * <p>Version 9 is identical to version 8 (paletted multi-layer) except that after the
 * layer-count byte there is an extra signed sectionY byte. This format is used by vanilla
 * Bedrock 1.18+. We skip the sectionY byte (we already know the section's position
 * from the LevelDB key) and then read the layers exactly as version 8 does.
 *
 * <p>Note: version 9 is the <em>on-disk</em> variant. The network format also uses version 9
 * but is handled separately in {@link org.cloudburstmc.server.level.chunk.CloudChunkSection#writeToNetwork}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ChunkSectionSerializerV9 implements ChunkSectionSerializer {

    static final ChunkSectionSerializer INSTANCE = new ChunkSectionSerializerV9();

    @Override
    public void serialize(ByteBuf buf, BlockStorage[] storage) {
        throw new UnsupportedOperationException("Version 9 on-disk serialization is not supported; use version 8.");
    }

    @Override
    public BlockStorage[] deserialize(ByteBuf buf, ChunkBuilder builder) {
        int storageCount = buf.readUnsignedByte();
        buf.readByte();

        BlockStorage[] storage = new BlockStorage[Math.max(storageCount, 2)];
        for (int i = 0; i < storageCount; i++) {
            storage[i] = new BlockStorage();
            storage[i].readFromStorage(buf);
        }
        return storage;
    }
}
