package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.daporkchop.ldbjni.direct.DirectDB;
import net.daporkchop.ldbjni.direct.DirectWriteBatch;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.server.level.chunk.BiomeStorage;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkException;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ChunkSerializerV3 implements ChunkSerializer {

    static final ChunkSerializer INSTANCE = new ChunkSerializerV3();
    private static final BiomeStorage DEFAULT_BIOME_STORAGE = new BiomeStorage(CloudChunkSection.DEFAULT_BIOME_ID);

    @Override
    public void serialize(DirectWriteBatch db, Chunk chunk) {
        int sectionCount = chunk.getLevel().getSectionsCount();
        int minSectionY = chunk.getLevel().getMinSectionY();

        BiomeStorage previousBiome = null;
        ByteBuf biomeBuffer = ByteBufAllocator.DEFAULT.ioBuffer();
        ByteBuf biomeKeyBuffer = ByteBufAllocator.DEFAULT.ioBuffer();
        ByteBuf subchunkKeyBuffer = ByteBufAllocator.DEFAULT.ioBuffer(10, 10);

        try {
            for (int arrayIndex = 0; arrayIndex < sectionCount; arrayIndex++) {
                CloudChunkSection section = (CloudChunkSection) chunk.getSection(arrayIndex);
                int absoluteSectionY = arrayIndex + minSectionY;

                if (section != null) {
                    ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
                    try {
                        section.writeToDisk(buffer);
                        subchunkKeyBuffer.clear();
                        LevelDBKey.SUBCHUNK_PREFIX.writeTo(subchunkKeyBuffer, chunk.getX(), chunk.getZ(), absoluteSectionY);
                        db.put(subchunkKeyBuffer, buffer);
                    } finally {
                        buffer.release();
                    }
                }

                BiomeStorage bs = (section != null) ? section.getBiomeStorage() : DEFAULT_BIOME_STORAGE;
                bs.writeToDisk(biomeBuffer, previousBiome);
                previousBiome = bs;
            }

            biomeKeyBuffer.clear();
            LevelDBKey.BIOME_STATE.writeTo(biomeKeyBuffer, chunk.getX(), chunk.getZ());
            db.put(biomeKeyBuffer, biomeBuffer);
        } finally {
            biomeBuffer.release();
            biomeKeyBuffer.release();
            subchunkKeyBuffer.release();
        }
    }

    @Override
    public void deserialize(DirectDB db, CloudChunkBuilder chunkBuilder) {
        int chunkX = chunkBuilder.getX();
        int chunkZ = chunkBuilder.getZ();

        byte[] extraData = db.get(LevelDBKey.BLOCK_EXTRA_DATA.getKey(chunkX, chunkZ));
        if (extraData != null) {
            throw new UnsupportedOperationException("Numeric block extra data is no longer supported in chunk " + chunkX + "," + chunkZ);
        }

        int sectionCount = chunkBuilder.getLevel().getSectionsCount();
        int minSectionY = chunkBuilder.getLevel().getMinSectionY();
        CloudChunkSection[] sections = new CloudChunkSection[sectionCount];

        // Chunk versions 24-26 stored subchunk keys with a +4 Y offset.
        int chunkVersion = chunkBuilder.getStorageVersion();
        int subChunkKeyOffset = (chunkVersion >= 24 && chunkVersion <= 26) ? 4 : 0;

        int maxSectionY = minSectionY + sectionCount - 1;

        ByteBuf subchunkKeyBuf = ByteBufAllocator.DEFAULT.ioBuffer(10, 10);
        try {
            for (int absoluteSectionY = minSectionY; absoluteSectionY <= maxSectionY; absoluteSectionY++) {
                subchunkKeyBuf.clear();
                LevelDBKey.SUBCHUNK_PREFIX.writeTo(subchunkKeyBuf, chunkX, chunkZ, (absoluteSectionY + subChunkKeyOffset) & 0xFF);

                ByteBuf buf = db.getZeroCopy(subchunkKeyBuf);
                if (buf == null) {
                    continue;
                }

                int arrayIndex = absoluteSectionY - minSectionY;

                try {
                    if (!buf.isReadable()) {
                        throw new ChunkException("Empty sub-chunk " + absoluteSectionY);
                    }

                    int subChunkVersion = buf.readUnsignedByte();
                    if (subChunkVersion < 8) {
                        chunkBuilder.markDirty();
                    }

                    BlockStorage[] blockStorage = ChunkSectionSerializers.deserialize(buf, chunkBuilder, subChunkVersion);
                    sections[arrayIndex] = new CloudChunkSection(chunkBuilder.getLevel().getServer().getBlockRegistry(), blockStorage);
                } finally {
                    buf.release();
                }
            }
        } finally {
            subchunkKeyBuf.release();
        }

        chunkBuilder.setSections(sections);

        ByteBuf biomeKeyBuf = ByteBufAllocator.DEFAULT.ioBuffer(9, 9);
        try {
            LevelDBKey.BIOME_STATE.writeTo(biomeKeyBuf, chunkX, chunkZ);
            ByteBuf biomeBuf = db.getZeroCopy(biomeKeyBuf);
            int biomeDataOffset = 0;

            if (biomeBuf == null) {
                biomeKeyBuf.clear();
                LevelDBKey.DATA_3D.writeTo(biomeKeyBuf, chunkX, chunkZ);
                biomeBuf = db.getZeroCopy(biomeKeyBuf);
                biomeDataOffset = 512; // skip heightmap
            }

            if (biomeBuf != null) {
                try {
                    biomeBuf.skipBytes(Math.min(biomeDataOffset, biomeBuf.readableBytes()));
                    BiomeStorage previous = null;
                    for (int arrayIndex = 0; arrayIndex < sectionCount; arrayIndex++) {
                        if (!biomeBuf.isReadable()) {
                            break;
                        }
                        BiomeStorage bs = BiomeStorage.readFromDisk(biomeBuf, previous);
                        if (sections[arrayIndex] == null) {
                            sections[arrayIndex] = new CloudChunkSection(
                                    chunkBuilder.getLevel().getServer().getBlockRegistry(),
                                    new BlockStorage[]{new BlockStorage(), new BlockStorage()});
                        }
                        sections[arrayIndex].setBiomeStorage(bs);
                        previous = bs;
                    }
                } finally {
                    biomeBuf.release();
                }
            }
        } finally {
            biomeKeyBuf.release();
        }
    }
}
