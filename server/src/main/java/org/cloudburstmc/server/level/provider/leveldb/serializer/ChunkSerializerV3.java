package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ShortMap;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.ldbjni.direct.DirectWriteBatch;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.chunk.ChunkException;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ChunkSerializerV3 extends ChunkSerializerV1 {

    static ChunkSerializer INSTANCE = new ChunkSerializerV3();

    @Override
    public void serialize(DirectWriteBatch db, Chunk chunk) {
        // LevelDB key byte is the absolute section Y
        for (int arrayIndex = 0; arrayIndex < CloudChunk.SECTION_COUNT; arrayIndex++) {
            CloudChunkSection section = (CloudChunkSection) chunk.getSection(arrayIndex);
            if (section == null) {
                continue;
            }

            int absoluteSectionY = arrayIndex + CloudChunk.MIN_SECTION_Y;

            ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
            ByteBuf keyBuffer = ByteBufAllocator.DEFAULT.ioBuffer();
            try {
                section.writeToDisk(buffer);
                keyBuffer.clear().writeBytes(LevelDBKey.SUBCHUNK_PREFIX.getKey(chunk.getX(), chunk.getZ(), absoluteSectionY));
                db.put(keyBuffer, buffer);
            } finally {
                keyBuffer.release();
                buffer.release();
            }
        }
    }

    @Override
    public void deserialize(DirectDB db, ChunkBuilder chunkBuilder) {
        int chunkX = chunkBuilder.getX();
        int chunkZ = chunkBuilder.getZ();

        Int2ShortMap extraDataMap = null;

        byte[] extraData = db.get(LevelDBKey.BLOCK_EXTRA_DATA.getKey(chunkX, chunkZ));
        if (extraData != null) {
            extraDataMap = new Int2ShortOpenHashMap();
            ByteBuf extraDataBuf = Unpooled.wrappedBuffer(extraData);

            int count = extraDataBuf.readIntLE();
            for (int i = 0; i < count; i++) {
                int key = extraDataBuf.readIntLE();
                short value = extraDataBuf.readShortLE();

                extraDataMap.put(key, value);
            }
        }

        CloudChunkSection[] sections = new CloudChunkSection[CloudChunk.SECTION_COUNT];

        // Key byte is absolute section Y. Pre-1.18 worlds used keys 0..15, which map
        // correctly to array indices 4..19 (world Y 0..240) under this scheme.
        int minSectionY = CloudChunk.MIN_SECTION_Y;
        int maxSectionY = minSectionY + CloudChunk.SECTION_COUNT - 1;

        for (int absoluteSectionY = minSectionY; absoluteSectionY <= maxSectionY; absoluteSectionY++) {
            ByteBuf buf = db.getZeroCopy(Unpooled.wrappedBuffer(LevelDBKey.SUBCHUNK_PREFIX.getKey(chunkX, chunkZ, absoluteSectionY)));
            if (buf == null) {
                continue;
            }

            int arrayIndex = absoluteSectionY - minSectionY;

            try {
                if (!buf.isReadable()) {
                    throw new ChunkException("Empty sub-chunk " + absoluteSectionY);
                }

                int subChunkVersion = buf.readUnsignedByte();
                // On-disk format uses version 8; mark dirty only if older than that
                if (subChunkVersion < 8) {
                    chunkBuilder.dirty();
                }

                BlockStorage[] blockStorage = ChunkSectionSerializers.deserialize(buf, chunkBuilder, subChunkVersion);
                if (blockStorage[1] == null) {
                    blockStorage[1] = new BlockStorage();
                    if (extraDataMap != null) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = absoluteSectionY * 16, lim = y + 16; y < lim; y++) {
                                    int key = CloudChunk.blockKey(x, y, z);
                                    if (extraDataMap.containsKey(key)) {
                                        short value = extraDataMap.get(CloudChunk.blockKey(x, y, z));
                                        int blockId = value & 0xff;
                                        int blockData = (value >> 8) & 0xf;
                                        blockStorage[1].setBlock(CloudChunkSection.blockIndex(x, y, z), CloudBlockRegistry.REGISTRY.getBlock(blockId, blockData));
                                    }
                                }
                            }
                        }
                    }
                }
                sections[arrayIndex] = new CloudChunkSection(blockStorage);
            } finally {
                buf.release();
            }
        }

        chunkBuilder.sections(sections);
    }

    @Override
    protected int deserializeExtraDataKey(int key) {
        return key;
    }
}
