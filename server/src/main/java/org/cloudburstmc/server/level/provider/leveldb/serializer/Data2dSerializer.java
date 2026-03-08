package org.cloudburstmc.server.level.provider.leveldb.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.server.level.chunk.ChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.provider.leveldb.LevelDBKey;
import org.iq80.leveldb.DB;

public class Data2dSerializer {

    // 512-byte heightmap + 256-byte 2D biome column (pre-3D biomes)
    private static final int LEGACY_DATA2D_SIZE = 768;

    public static void deserialize(DB db, ChunkBuilder builder) {
        byte[] data2d = db.get(LevelDBKey.DATA_2D.getKey(builder.getX(), builder.getZ()));
        int[] heightMap = new int[256];

        if (data2d != null) {
            ByteBuf buffer = Unpooled.wrappedBuffer(data2d);
            try {
                int heightMapCount = Math.min(256, buffer.readableBytes() / 2);
                for (int i = 0; i < heightMapCount; i++) {
                    heightMap[i] = buffer.readUnsignedShortLE();
                }

                // Legacy DATA_2D (pre-3D biomes): 512-byte heightmap + 256-byte 2D biome column.
                // Expand into per-section 3D biome storage via a deferred loader; return true
                // to mark the chunk dirty so the upgraded format is written on next save.
                if (data2d.length >= LEGACY_DATA2D_SIZE) {
                    final byte[] biomesRaw = new byte[256];
                    buffer.readerIndex(512);
                    int biomeCount = Math.min(256, buffer.readableBytes());
                    buffer.readBytes(biomesRaw, 0, biomeCount);

                    builder.dataLoader(chunk -> {
                        int sectionCount = chunk.getLevel().getSectionsCount();
                        for (int i = 0; i < sectionCount; i++) {
                            CloudChunkSection section = (CloudChunkSection) chunk.getSection(i);
                            if (section == null) continue;
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    int biomeId = biomesRaw[z * 16 + x] & 0xFF;
                                    section.fillColumnBiome(x, z, biomeId);
                                }
                            }
                        }
                        return true; // upgraded from 2D biomes; mark dirty
                    });
                }
            } finally {
                buffer.release();
            }
        }

        builder.heightMap(heightMap);
    }
}
