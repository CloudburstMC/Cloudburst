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
import org.cloudburstmc.server.level.chunk.*;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ChunkSerializerV3 extends ChunkSerializerV1 {

    static final ChunkSerializer INSTANCE = new ChunkSerializerV3();
    private static final BiomeStorage DEFAULT_BIOME_STORAGE = new BiomeStorage(CloudChunkSection.DEFAULT_BIOME_ID);

    @Override
    public void serialize(DirectWriteBatch db, Chunk chunk) {
        int sectionCount = chunk.getLevel().getSectionsCount();
        int minSectionY = chunk.getLevel().getMinSectionY();

        // Write block sub-chunks and biome sections together.
        // BIOME_STATE uses the same copy-last optimization as the network path.
        BiomeStorage previousBiome = null;
        ByteBuf biomeBuffer = ByteBufAllocator.DEFAULT.ioBuffer();
        ByteBuf biomeKeyBuffer = ByteBufAllocator.DEFAULT.ioBuffer();

        try {
            for (int arrayIndex = 0; arrayIndex < sectionCount; arrayIndex++) {
                CloudChunkSection section = (CloudChunkSection) chunk.getSection(arrayIndex);
                int absoluteSectionY = arrayIndex + minSectionY;

                // Block storage
                if (section != null) {
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

                // Biome storage: write one entry per section into the BIOME_STATE buffer
                BiomeStorage bs = (section != null) ? section.getBiomeStorage() : DEFAULT_BIOME_STORAGE;
                bs.writeToDisk(biomeBuffer, previousBiome);
                previousBiome = bs;
            }

            // Write the full biome buffer as a single BIOME_STATE key
            biomeKeyBuffer.clear().writeBytes(LevelDBKey.BIOME_STATE.getKey(chunk.getX(), chunk.getZ()));
            db.put(biomeKeyBuffer, biomeBuffer);
        } finally {
            biomeBuffer.release();
            biomeKeyBuffer.release();
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

        int sectionCount = chunkBuilder.getLevel().getSectionsCount();
        int minSectionY = chunkBuilder.getLevel().getMinSectionY();
        CloudChunkSection[] sections = new CloudChunkSection[sectionCount];

        // Key byte is absolute section Y. Pre-1.18 worlds used keys 0..15, which map
        // correctly to array indices 4..19 (world Y 0..240) under this scheme.
        int maxSectionY = minSectionY + sectionCount - 1;

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
                                    int key = CloudChunk.blockKey(x, y, z, chunkBuilder.getLevel().getMinHeight());
                                    if (extraDataMap.containsKey(key)) {
                                        short value = extraDataMap.get(key);
                                        int blockId = value & 0xff;
                                        int blockData = (value >> 8) & 0xf;
                                        blockStorage[1].setBlock(CloudChunkSection.blockIndex(x, y & 0xf, z),
                                                CloudBlockRegistry.REGISTRY.getBlock(blockId, blockData));
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

        byte[] biomeData = db.get(LevelDBKey.BIOME_STATE.getKey(chunkX, chunkZ));
        if (biomeData != null) {
            ByteBuf biomeBuf = Unpooled.wrappedBuffer(biomeData);
            BiomeStorage previous = null;
            for (int arrayIndex = 0; arrayIndex < sectionCount; arrayIndex++) {
                if (!biomeBuf.isReadable()) {
                    break;
                }
                BiomeStorage bs = BiomeStorage.readFromDisk(biomeBuf, previous);
                if (sections[arrayIndex] == null) {
                    // Section had no block data but has biome data, so create a section to hold it
                    sections[arrayIndex] = new CloudChunkSection(new BlockStorage[]{new BlockStorage(), new BlockStorage()});
                }
                sections[arrayIndex].setBiomeStorage(bs);
                previous = bs;
            }
        }
        // If no BIOME_STATE key exists, sections keep their default BiomeStorage (Ocean = 0).
    }
}
