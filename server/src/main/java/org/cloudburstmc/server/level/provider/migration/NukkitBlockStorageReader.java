package org.cloudburstmc.server.level.provider.migration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.CloudChunkSection;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.level.chunk.bitarray.BitArrayVersion;
import org.cloudburstmc.server.utils.NibbleArray;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Decodes numeric and named Nukkit block layers for world import.
 */
public class NukkitBlockStorageReader {

    private static final int SIZE = 4096;
    private final NukkitBlockStateConverter blocks;

    public NukkitBlockStorageReader(NukkitBlockStateConverter blocks) {
        this.blocks = Objects.requireNonNull(blocks, "blocks");
    }

    /**
     * Applies saved secondary blocks to the destination sections, rejecting conflicting states.
     * Missing sections are created within the supplied height range.
     */
    public void readAnvilExtraData(byte[] data, CloudChunkSection[] sections, int minSectionY, BlockRegistry registry) throws IOException {
        if (data.length == 0) {
            return;
        }

        if (data.length < Integer.BYTES) {
            throw new IOException("Truncated Nukkit Anvil ExtraData count");
        }

        ByteBuffer input = ByteBuffer.wrap(data);
        int count = input.getInt();
        if (count < 0 || count > 16 * 16 * 512 || input.remaining() != count * 6) {
            throw new IOException("Invalid Nukkit Anvil ExtraData count or length");
        }

        for (int i = 0; i < count; i++) {
            int key = input.getInt();
            int value = Short.toUnsignedInt(input.getShort());
            if (key >>> 17 != 0) {
                throw new IOException("Invalid Nukkit Anvil ExtraData position: " + key);
            }

            int x = key >>> 13 & 15;
            int z = key >>> 9 & 15;
            int y = (key & 511) - 64;
            int sectionIndex = (y >> 4) - minSectionY;
            if (sectionIndex < 0 || sectionIndex >= sections.length) {
                throw new IOException("Nukkit Anvil ExtraData is outside the destination height: " + y);
            }

            BlockState state;
            try {
                state = this.blocks.read(value & 0xff, value >>> 8);
            } catch (IllegalArgumentException failure) {
                throw new IOException("Invalid Nukkit Anvil ExtraData block at " + x + "," + y + "," + z, failure);
            }

            CloudChunkSection section = sections[sectionIndex];
            if (section == null) {
                section = new CloudChunkSection(registry);
                sections[sectionIndex] = section;
            }

            BlockState existing = section.getBlockState(x, y & 15, z, BlockLayer.SECONDARY);
            if (existing.getType() != BlockTypes.AIR && existing != state) {
                throw new IOException("Conflicting Nukkit Anvil secondary block at " + x + "," + y + "," + z);
            }

            section.setBlockState(x, y & 15, z, BlockLayer.SECONDARY, state);
        }
    }

    /**
     * Decodes an Anvil section using its enclosing chunk's encoding version.
     */
    public BlockStorage[] readAnvilSection(NbtMap section, int version) throws IOException {
        try {
            return switch (version) {
                case 0, 1 -> readAnvilBytes(section);
                case 2, 3, 4 -> readAnvilPacked(section);
                default -> throw new IOException("Unknown Nukkit Anvil chunk version: " + version);
            };
        } catch (IllegalArgumentException | IndexOutOfBoundsException failure) {
            throw new IOException("Invalid Nukkit Anvil section " + section.getByte("Y") + " for version " + version, failure);
        }
    }

    private BlockStorage[] readAnvilBytes(NbtMap section) throws IOException {
        byte[] ids = section.getByteArray("Blocks");
        byte[] metadata = section.getByteArray("Data");
        if (ids.length != SIZE || metadata.length != SIZE / 2
                || section.containsKey("layer1") || section.containsKey("layer2")
                || section.containsKey("Blocks2PM1E") || section.containsKey("Blocks3PM1E")) {
            throw new IOException("Invalid or unsupported byte-array Nukkit Anvil section " + section.getByte("Y"));
        }

        NibbleArray data = new NibbleArray(metadata);
        BlockStorage primary = new BlockStorage();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    int source = y << 8 | z << 4 | x;
                    int target = x << 8 | z << 4 | y;
                    primary.setBlock(target, this.blocks.read(ids[source] & 0xff, data.get(source)));
                }
            }
        }

        return new BlockStorage[]{primary, new BlockStorage()};
    }

    private BlockStorage[] readAnvilPacked(NbtMap section) throws IOException {
        if (section.containsKey("Blocks") || section.containsKey("Data")) {
            throw new IOException("Packed Nukkit Anvil sections must not contain byte-array block data");
        }

        BlockStorage[] result = new BlockStorage[]{new BlockStorage(), new BlockStorage()};
        for (int layer = 0; layer < result.length; layer++) {
            String name = "layer" + (layer + 1);
            int[] entries = section.getIntArray(name);
            if (entries.length != SIZE) {
                throw new IOException("Nukkit Anvil " + name + " must contain " + SIZE + " entries");
            }

            for (int i = 0; i < SIZE; i++) {
                result[layer].setBlock(i, this.blocks.read(entries[i] >>> 6, entries[i] & 0x3f));
            }
        }

        return result;
    }

    /**
     * Decodes a complete LevelDB sub-chunk at its absolute section Y coordinate.
     */
    public BlockStorage[] readSection(byte[] data, int sectionY) throws IOException {
        ByteBuf input = Unpooled.wrappedBuffer(data);
        try {
            int version = input.readUnsignedByte();
            BlockStorage[] result;
            switch (version) {
                case 0, 2, 3, 4, 5, 6, 7 -> result = new BlockStorage[]{readNumeric(input), new BlockStorage()};
                case 1 -> result = new BlockStorage[]{readPalette(input), new BlockStorage()};
                case 8, 9 -> {
                    int count = input.readUnsignedByte();
                    if (count > 2) {
                        throw new IOException("Unsupported Nukkit block layer count: " + count);
                    }

                    if (version == 9 && input.readByte() != sectionY) {
                        throw new IOException("Nukkit sub-chunk Y does not match its key");
                    }

                    result = new BlockStorage[]{new BlockStorage(), new BlockStorage()};
                    for (int layer = 0; layer < count; layer++) {
                        result[layer] = readPalette(input);
                    }
                }
                default -> throw new IOException("Unknown Nukkit sub-chunk version: " + version);
            }

            if (input.isReadable()) {
                throw new IOException("Unexpected trailing data in Nukkit sub-chunk");
            }

            return result;
        } catch (IllegalArgumentException | IndexOutOfBoundsException failure) {
            throw new IOException("Invalid Nukkit sub-chunk at Y " + sectionY, failure);
        } finally {
            input.release();
        }
    }

    private BlockStorage readNumeric(ByteBuf input) throws IOException {
        byte[] ids = new byte[SIZE];
        byte[] metadata = new byte[SIZE / 2];
        input.readBytes(ids).readBytes(metadata);
        if (input.readableBytes() == SIZE) {
            input.skipBytes(SIZE);
        } else if (input.isReadable()) {
            throw new IOException("Invalid numeric Nukkit sub-chunk light data");
        }

        BlockStorage storage = new BlockStorage();
        for (int i = 0; i < SIZE; i++) {
            int meta = (metadata[i >> 1] >>> ((i & 1) * 4)) & 0xf;
            storage.setBlock(i, this.blocks.read(ids[i] & 0xff, meta));
        }

        return storage;
    }

    private BlockStorage readPalette(ByteBuf input) throws IOException {
        int header = input.readUnsignedByte();
        if ((header & 1) != 0) {
            throw new IOException("Runtime-ID palette found in persisted Nukkit data");
        }

        int bits = header >>> 1;
        BitArray indices = null;
        int count = 1;
        if (bits != 0) {
            BitArrayVersion version = BitArrayVersion.fromBits(bits);
            int[] words = new int[version.wordCount(SIZE)];
            for (int i = 0; i < words.length; i++) {
                words[i] = input.readIntLE();
            }

            indices = version.createArray(SIZE, words);
            count = input.readIntLE();
            if (count <= 0 || count > SIZE || count - 1 > version.getMaxValue()) {
                throw new IOException("Invalid Nukkit block palette size: " + count);
            }
        }

        BlockState[] palette = new BlockState[count];
        try (NBTInputStream nbt = NbtUtils.createReaderLE(new ByteBufInputStream(input))) {
            for (int i = 0; i < count; i++) {
                if (!(nbt.readTag() instanceof NbtMap tag)) {
                    throw new IOException("Nukkit palette entry must be a compound");
                }

                palette[i] = this.blocks.readNamed(tag);
            }
        }

        BlockStorage storage = new BlockStorage();
        for (int i = 0; i < SIZE; i++) {
            int index = indices == null ? 0 : indices.get(i);
            if (index >= count) {
                throw new IOException("Nukkit palette index is outside the palette");
            }

            storage.setBlock(i, palette[index]);
        }

        return storage;
    }
}
