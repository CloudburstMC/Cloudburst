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
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.ChunkSection;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.ChunkException;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ChunkSerializerV3 extends ChunkSerializerV1 {

    static ChunkSerializer INSTANCE = new ChunkSerializerV3();

    @Override
    public void serialize(DirectWriteBatch db, Chunk chunk) {
        // Write chunk sections
        for (int ySection = 0; ySection < Chunk.SECTION_COUNT; ySection++) {
            ChunkSection section = chunk.getSection(ySection);
            if (section == null) {
                continue;
            }

            ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
            ByteBuf keyBuffer = ByteBufAllocator.DEFAULT.ioBuffer();
            try {
                buffer.writeByte(ChunkSection.CHUNK_SECTION_VERSION);
                ChunkSectionSerializers.serialize(buffer, section.getBlockStorageArray(), ChunkSection.CHUNK_SECTION_VERSION);

                keyBuffer.clear().writeBytes(LevelDBKey.SUBCHUNK_PREFIX.getKey(chunk.getX(), chunk.getZ(), ySection));
                db.put(keyBuffer, buffer);

                buffer.clear(); //reset indices to prevent the buffer from constantly growing
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

        ChunkSection[] sections = new ChunkSection[Chunk.SECTION_COUNT];

        for (int ySection = 0; ySection < Chunk.SECTION_COUNT; ySection++) {
            ByteBuf buf = db.getZeroCopy(Unpooled.wrappedBuffer(LevelDBKey.SUBCHUNK_PREFIX.getKey(chunkX, chunkZ, ySection)));
            if (buf == null)    {
                continue; //entry doesn't exist, skip
            }
            try {
                if (!buf.isReadable()) {
                    throw new ChunkException("Empty sub-chunk " + ySection);
                }

                int subChunkVersion = buf.readUnsignedByte();
                if (subChunkVersion < ChunkSection.CHUNK_SECTION_VERSION) {
                    chunkBuilder.dirty();
                }
                BlockStorage[] blockStorage = ChunkSectionSerializers.deserialize(buf, chunkBuilder, subChunkVersion);

                if (blockStorage[1] == null) {
                    blockStorage[1] = new BlockStorage();
                    if (extraDataMap != null) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = ySection * 16, lim = y + 16; y < lim; y++) {
                                    int key = Chunk.blockKey(x, y, z);
                                    if (extraDataMap.containsKey(key)) {
                                        short value = extraDataMap.get(Chunk.blockKey(x, y, z));
                                        int blockId = value & 0xff;
                                        int blockData = (value >> 8) & 0xf;
                                        blockStorage[1].setBlock(ChunkSection.blockIndex(x, y, z), BlockRegistry.get().getBlock(blockId, blockData));
                                    }
                                }
                            }
                        }
                    }
                }
                sections[ySection] = new ChunkSection(blockStorage);
            } finally {
                buf.release(); //release buffer to avoid memory leak
            }
        }

        chunkBuilder.sections(sections);
    }

    @Override
    protected int deserializeExtraDataKey(int key) {
        return key;
    }
}
