package org.cloudburstmc.server.level.provider.migration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.level.chunk.BiomeStorage;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;
import org.cloudburstmc.server.level.chunk.CloudChunkBuilder;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.provider.ImportedChunk;
import org.iq80.leveldb.DB;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Reads a Nukkit LevelDB chunk into named Cloudburst storage.
 */
public class NukkitLevelDBChunkReader {

    private final DB database;
    private final int dimension;
    private final NukkitBlockStateConverter blocks;
    private final NukkitItemDataConverter items;
    private final NukkitBlockStorageReader storage;

    public NukkitLevelDBChunkReader(DB database, int dimension, NukkitBlockStateConverter blocks, NukkitItemDataConverter items) {
        this.database = Objects.requireNonNull(database, "database");
        this.dimension = dimension;
        this.blocks = Objects.requireNonNull(blocks, "blocks");
        this.items = Objects.requireNonNull(items, "items");
        this.storage = new NukkitBlockStorageReader(blocks);
    }

    public ImportedChunk read(CloudChunkBuilder builder) throws IOException {
        byte[] versionTag = get(builder, ',');
        if (versionTag == null) {
            versionTag = get(builder, 'v');
        }

        if (versionTag == null || versionTag.length != 1) {
            throw new IOException("Missing or invalid Nukkit chunk version at " + builder.getX() + "," + builder.getZ());
        }

        int version = versionTag[0] & 0xff;
        if (version > 42) {
            throw new IOException("Unknown Nukkit chunk version: " + version);
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
        builder.setGenerationStatus(generationStatus(get(builder, '6')));
        return new ImportedChunk(builder.build(),
                NukkitEntityData.entities(builder, compounds(get(builder, '2')), this.blocks, this.items),
                NukkitEntityData.blockEntities(builder, compounds(get(builder, '1')), this.blocks, this.items),
                readTicks(builder, get(builder, '3'))
        );
    }

    private byte[] get(CloudChunkBuilder builder, int tag) {
        return this.database.get(NukkitLevelDBKeys.key(builder.getX(), builder.getZ(), this.dimension, tag));
    }

    private void readSections(CloudChunkBuilder builder, CloudChunkSection[] sections, int version) throws IOException {
        for (int keyY = Byte.MIN_VALUE; keyY <= Byte.MAX_VALUE; keyY++) {
            byte[] data = this.database.get(NukkitLevelDBKeys.section(builder.getX(), builder.getZ(), this.dimension, keyY));
            if (data == null) {
                continue;
            }

            int y = version >= 24 && version <= 26 ? keyY - 4 : keyY;
            int index = sectionIndex(builder, y);
            sections[index] = new CloudChunkSection(builder.getLevel().getServer().getBlockRegistry(), this.storage.readSection(data, y));
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
                    section.setBlockState(x, y & 15, z, BlockLayer.PRIMARY, this.blocks.read(terrain[index] & 0xff, metadata));
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
            throw new IOException("Nukkit chunk has no biome data");
        }

        ByteBuf input = Unpooled.wrappedBuffer(data);
        try {
            for (int i = 0; i < 256; i++) {
                heights[i] = input.readUnsignedShortLE();
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
                throw new IOException("Unexpected trailing Nukkit biome data");
            }
        } catch (IndexOutOfBoundsException | IllegalArgumentException failure) {
            throw new IOException("Invalid Nukkit biome data", failure);
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
                BlockState state = this.blocks.read(value & 0xff, value >>> 8 & 15);
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

    private NbtMap readTicks(CloudChunkBuilder builder, byte[] data) throws IOException {
        if (data == null) {
            return NbtMap.EMPTY;
        }

        List<NbtMap> roots = compounds(data);
        if (roots.size() != 1) {
            throw new IOException("Pending ticks must contain one root compound");
        }

        NbtMap root = roots.getFirst();
        List<NbtMap> ticks = new ArrayList<>();
        for (NbtMap tag : root.getList("tickList", NbtType.COMPOUND)) {
            Vector3i position = Vector3i.from(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            if ((position.getX() >> 4) != builder.getX() || (position.getZ() >> 4) != builder.getZ()) {
                throw new IOException("Pending tick is outside its chunk");
            }

            sectionIndex(builder, position.getY() >> 4);
            if (!tag.containsKey("blockState") && !tag.containsKey("tileID")) {
                throw new IOException("Pending tick has no block reference");
            }

            BlockState state = tag.containsKey("blockState") ? this.blocks.readNamed(tag.getCompound("blockState")) : this.blocks.read(tag.getByte("tileID") & 0xff, 0);
            long remaining = Math.max(0, Math.subtractExact(tag.getLong("time"), root.getInt("currentTick")));
            ticks.add(tag.toBuilder().putCompound("blockState", BlockStateCodec.write(state)).putLong("time", remaining).build());
        }

        return NbtMap.builder().putInt("currentTick", 0).putList("tickList", NbtType.COMPOUND, ticks).build();
    }

    private static void fillBiomes(CloudChunkBuilder builder, CloudChunkSection[] sections, byte[] biomes) throws IOException {
        for (int y = 0; y < sections.length; y++) {
            CloudChunkSection section = section(builder, sections, y + builder.getLevel().getMinSectionY());
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    section.fillColumnBiome(x, z, biomes[z * 16 + x] & 0xff);
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

    private static ChunkGenerationStatus generationStatus(byte[] data) throws IOException {
        if (data == null) {
            return ChunkGenerationStatus.FINISHED;
        }

        if (data.length != Integer.BYTES) {
            throw new IOException("Invalid Nukkit finalization state");
        }

        int state = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
        return switch (state) {
            case -1 -> ChunkGenerationStatus.NEW;
            case 0 -> ChunkGenerationStatus.GENERATED;
            case 1 -> ChunkGenerationStatus.POPULATED;
            case 2 -> ChunkGenerationStatus.FINISHED;
            default -> throw new IOException("Unknown Nukkit finalization state: " + state);
        };
    }

    private static List<NbtMap> compounds(byte[] data) throws IOException {
        if (data == null) {
            return List.of();
        }

        List<NbtMap> tags = new ArrayList<>();
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(data);
             NBTInputStream input = NbtUtils.createReaderLE(bytes)) {
            while (bytes.available() > 0) {
                if (!(input.readTag() instanceof NbtMap tag)) {
                    throw new IOException("Nukkit entity data must contain compounds");
                }

                tags.add(tag);
            }
        }

        return tags;
    }
}
