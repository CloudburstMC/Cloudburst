package org.cloudburstmc.server.level.provider.migration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.level.chunk.bitarray.BitArrayVersion;

import java.io.IOException;
import java.util.Objects;

/**
 * Decodes numeric and named Nukkit sub-chunks without runtime compatibility paths.
 */
public class NukkitBlockStorageReader {

    private static final int SIZE = 4096;
    private final NukkitBlockStateConverter blocks;

    public NukkitBlockStorageReader(NukkitBlockStateConverter blocks) {
        this.blocks = Objects.requireNonNull(blocks, "blocks");
    }

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
