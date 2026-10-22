package org.cloudburstmc.server.level.provider.migration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.level.chunk.BiomeStorage;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.iq80.leveldb.DB;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Reads dimension-qualified LevelDB records into named Cloudburst storage.
 */
public class LevelDBChunkReader {

    private final DB database;
    private final int dimension;
    private final LevelDBChunkCodec codec;
    private final LevelDBPendingTickReader tickReader;
    private final LevelDBEntityReader entityReader;

    public LevelDBChunkReader(DB database, int dimension, LevelDBChunkCodec codec) {
        this.database = Objects.requireNonNull(database, "database");
        this.dimension = dimension;
        this.codec = Objects.requireNonNull(codec, "codec");
        this.tickReader = new LevelDBPendingTickReader(database, dimension, codec);
        this.entityReader = new LevelDBEntityReader(database, dimension);
    }

    /**
     * Decodes the source chunk at the builder's coordinates within the destination height range.
     * The caller owns the returned chunk and must close it after saving or consuming it.
     */
    public ImportedChunk read(CloudChunkBuilder builder) throws IOException {
        byte[] versionTag = get(builder, ',');
        if (versionTag == null) {
            versionTag = get(builder, 'v');
        }

        if (versionTag == null || versionTag.length != 1) {
            throw new IOException("Missing or invalid LevelDB chunk version at " + builder.getX() + "," + builder.getZ());
        }

        int version = versionTag[0] & 0xff;
        if (version > 42) {
            throw new IOException("Unknown LevelDB chunk version: " + version);
        }

        CloudChunkSection[] sections = new CloudChunkSection[builder.getLevel().getSectionsCount()];
        int[] heightMap = new int[256];
        Arrays.fill(heightMap, builder.getLevel().getMinHeight());
        if (version < 3) {
            readLegacyTerrain(builder, sections, heightMap);
        } else {
            readSections(builder, sections, version);
            readBiomes(builder, sections, heightMap);
        }

        readExtraBlocks(builder, sections, version);
        builder.setSections(sections);
        builder.setHeightMap(heightMap);
        builder.setGenerationStatus(this.codec.readGeneration(get(builder, '6')));

        List<NbtMap> entities = ImportedEntityData.entities(this.entityReader.readEntities(builder.getX(), builder.getZ()), this.codec::convertEntity);
        List<NbtMap> blockEntities = ImportedEntityData.blockEntities(builder, this.entityReader.readBlockEntities(builder.getX(), builder.getZ()), this.codec::convertBlockEntity);
        NbtMap pendingTicks = this.tickReader.read(builder.getX(), builder.getZ(), builder.getLevel().getMinHeight(), builder.getLevel().getMaxHeight());

        return new ImportedChunk(builder.build(), entities, blockEntities, pendingTicks);
    }

    private byte @Nullable [] get(CloudChunkBuilder builder, int tag) {
        return this.database.get(LevelDBImportKeys.key(builder.getX(), builder.getZ(), this.dimension, tag));
    }

    private void readSections(CloudChunkBuilder builder, CloudChunkSection[] sections, int version) throws IOException {
        for (int keyY = Byte.MIN_VALUE; keyY <= Byte.MAX_VALUE; keyY++) {
            byte[] data = this.database.get(LevelDBImportKeys.section(builder.getX(), builder.getZ(), this.dimension, keyY));
            if (data == null) {
                continue;
            }

            int y = version >= 24 && version <= 26 ? keyY - 4 : keyY;
            int index = sectionIndex(builder, y);
            sections[index] = new CloudChunkSection(builder.getLevel().getServer().getBlockRegistry(), this.codec.readSection(data, y));
        }
    }

    private void readLegacyTerrain(CloudChunkBuilder builder, CloudChunkSection[] sections, int[] heights) throws IOException {
        byte[] terrain = get(builder, '0');
        if (terrain == null || terrain.length != 83200) {
            throw new IOException("Invalid legacy whole-chunk terrain");
        }

        for (int y = 0; y < 128; y++) {
            CloudChunkSection section = section(builder, sections, y >> 4);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int index = x << 11 | z << 7 | y;
                    int metadata = (terrain[32768 + (index >> 1)] >>> ((index & 1) * 4)) & 0xf;
                    section.setBlockState(x, y & 15, z, BlockLayer.PRIMARY, this.codec.readLegacyBlock(terrain[index] & 0xff, metadata));
                }
            }
        }

        ByteBuf tail = Unpooled.wrappedBuffer(terrain, 81920, 1280);
        try {
            for (int i = 0; i < 256; i++) {
                heights[i] = tail.readUnsignedByte();
            }

            byte[] biomes = new byte[256];
            for (int i = 0; i < 256; i++) {
                biomes[i] = (byte) (tail.readInt() >>> 24);
            }

            fillBiomes(builder, sections, biomes);
        } finally {
            tail.release();
        }
    }

    private void readBiomes(CloudChunkBuilder builder, CloudChunkSection[] sections, int[] heights) throws IOException {
        byte[] data = get(builder, '+');
        boolean threeDimensional = data != null;
        if (data == null) {
            data = get(builder, '-');
        }

        if (data == null) {
            throw new IOException("LevelDB chunk has no biome data");
        }

        ByteBuf input = Unpooled.wrappedBuffer(data);
        try {
            for (int i = 0; i < 256; i++) {
                heights[i] = this.codec.readHeight(input.readUnsignedShortLE(), builder.getLevel().getMinHeight(), threeDimensional);
            }

            if (threeDimensional) {
                BiomeStorage previous = null;
                for (int i = 0; i < sections.length; i++) {
                    previous = BiomeStorage.readFromDisk(input, previous);
                    section(builder, sections, i + builder.getLevel().getMinSectionY()).setBiomeStorage(previous);
                }
            } else {
                byte[] biomes = new byte[256];
                input.readBytes(biomes);
                fillBiomes(builder, sections, biomes);
            }

            if (input.isReadable()) {
                throw new IOException("Unexpected trailing biome data");
            }
        } catch (IndexOutOfBoundsException | IllegalArgumentException failure) {
            throw new IOException("Invalid LevelDB biome data", failure);
        } finally {
            input.release();
        }
    }

    private void readExtraBlocks(CloudChunkBuilder builder, CloudChunkSection[] sections, int version) throws IOException {
        byte[] data = get(builder, '4');
        if (data == null || data.length == 0) {
            return;
        }

        ByteBuf input = Unpooled.wrappedBuffer(data);
        try {
            int count = input.readIntLE();
            if (count < 0 || count > 65536 || input.readableBytes() != count * 6) {
                throw new IOException("Invalid legacy block extra data count");
            }

            for (int i = 0; i < count; i++) {
                int key = input.readIntLE();
                int value = input.readUnsignedShortLE();
                int bits = version < 3 ? 7 : 8;

                int x = key >>> (bits + 4) & 15;
                int z = key >>> bits & 15;
                int y = key & ((1 << bits) - 1);

                CloudChunkSection section = section(builder, sections, y >> 4);
                BlockState state = this.codec.readLegacyBlock(value & 0xff, value >>> 8 & 15);
                BlockState existing = section.getBlockState(x, y & 15, z, BlockLayer.SECONDARY);
                if (existing.getType() != BlockTypes.AIR && existing != state) {
                    throw new IOException("Conflicting secondary block data at " + x + "," + y + "," + z);
                }

                section.setBlockState(x, y & 15, z, BlockLayer.SECONDARY, state);
            }
        } catch (IndexOutOfBoundsException failure) {
            throw new IOException("Truncated block extra data", failure);
        } finally {
            input.release();
        }
    }

    private void fillBiomes(CloudChunkBuilder builder, CloudChunkSection[] sections, byte[] biomes) throws IOException {
        for (int y = 0; y < sections.length; y++) {
            CloudChunkSection section = section(builder, sections, y + builder.getLevel().getMinSectionY());
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    section.fillColumnBiome(x, z, this.codec.readColumnBiome(biomes, x, z));
                }
            }
        }
    }

    private static CloudChunkSection section(CloudChunkBuilder builder, CloudChunkSection[] sections, int y) throws IOException {
        int index = sectionIndex(builder, y);
        if (sections[index] == null) {
            sections[index] = new CloudChunkSection(builder.getLevel().getServer().getBlockRegistry());
        }

        return sections[index];
    }

    private static int sectionIndex(CloudChunkBuilder builder, int y) throws IOException {
        int index = y - builder.getLevel().getMinSectionY();
        if (index < 0 || index >= builder.getLevel().getSectionsCount()) {
            throw new IOException("Imported section is outside the destination height: " + y);
        }

        return index;
    }
}
