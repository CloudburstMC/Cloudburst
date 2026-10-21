package org.cloudburstmc.server.level.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.level.chunk.bitarray.BitArrayVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BiomeStorageTest {

    private static final int SIZE = 4096;

    @Test
    void singletonAccessChecksBoundsAndCopiesRemainIndependent() {
        BiomeStorage storage = new BiomeStorage(3);

        assertEquals(3, storage.getBiome(0));
        assertEquals(3, storage.getBiome(SIZE - 1));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.getBiome(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.getBiome(SIZE));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.setBiome(-1, 9));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.setBiome(SIZE, 9));

        storage.setBiome(0, 3);
        BiomeStorage copy = storage.copy();
        copy.setBiome(0, 9);

        assertEquals(3, storage.getBiome(0));
        assertEquals(9, copy.getBiome(0));
        assertEquals(3, copy.getBiome(1));
        assertSingletonEncoding(storage, 3);
    }

    @Test
    void everyBitWidthPreservesValuesAcrossDiskAndNetworkEncoding() {
        int[] sizes = {1, 2, 4, 8, 16, 32, 64, 256, 300};
        int[] widths = {1, 2, 3, 4, 5, 6, 8, 16, 16};
        BiomeStorage storage = new BiomeStorage(0);
        int populated = 0;

        for (int step = 0; step < sizes.length; step++) {
            int count = sizes[step];
            while (populated < count) {
                storage.setBiome(populated, populated + 1);
                populated++;
            }

            ByteBuf network = Unpooled.buffer();
            ByteBuf disk = Unpooled.buffer();
            try {
                storage.writeToNetwork(network, null);
                assertEquals((widths[step] << 1) | 1, network.getUnsignedByte(0));
                int[] networkValues = readNetworkValues(network);
                assertFalse(network.isReadable());

                storage.writeToDisk(disk, null);
                assertEquals(widths[step] << 1, disk.getUnsignedByte(0));
                BiomeStorage restored = BiomeStorage.readFromDisk(disk, null);
                assertFalse(disk.isReadable());

                for (int index = 0; index < SIZE; index++) {
                    int expected = index < count ? index + 1 : 0;
                    assertEquals(expected, networkValues[index]);
                    assertEquals(expected, restored.getBiome(index));
                }
            } finally {
                network.release();
                disk.release();
            }
        }
    }

    @Test
    void revertingChangesRemovesStalePaletteEntries() {
        BiomeStorage storage = new BiomeStorage(4);
        storage.setBiome(20, 5);
        storage.setBiome(20, 4);

        assertSingletonEncoding(storage, 4);
    }

    @Test
    void copyLastComparesValuesDespiteDifferentInsertionHistories() {
        BiomeStorage previous = new BiomeStorage(10);
        previous.setBiome(0, 20);
        previous.setBiome(5, 30);
        BiomeStorage current = new BiomeStorage(10);
        current.setBiome(5, 30);
        current.setBiome(0, 20);

        ByteBuf disk = Unpooled.buffer();
        ByteBuf network = Unpooled.buffer();
        try {
            current.writeToDisk(disk, previous);
            assertEquals(0xFE, disk.getUnsignedByte(0));
            assertEquals(1, disk.readableBytes());
            BiomeStorage restored = BiomeStorage.readFromDisk(disk, previous);
            restored.setBiome(0, 40);

            assertEquals(20, previous.getBiome(0));
            assertEquals(20, current.getBiome(0));
            assertEquals(40, restored.getBiome(0));

            current.writeToNetwork(network, previous);
            assertEquals(0xFF, network.readUnsignedByte());
            assertFalse(network.isReadable());

            current.setBiome(5, 40);
            network.clear();
            current.writeToNetwork(network, previous);
            assertNotEquals(0xFF, network.getUnsignedByte(0));
            assertEquals(40, readNetworkValues(network)[5]);
        } finally {
            disk.release();
            network.release();
        }
    }

    @Test
    void networkCopyLastExclusionsDoNotRestrictDiskCopyLast() {
        for (int id : new int[]{0, 1}) {
            BiomeStorage previous = new BiomeStorage(id);
            BiomeStorage current = previous.copy();
            ByteBuf network = Unpooled.buffer();
            ByteBuf disk = Unpooled.buffer();
            try {
                current.writeToNetwork(network, previous);
                assertEquals(1, network.readUnsignedByte());
                assertEquals(id, VarInts.readInt(network));
                assertFalse(network.isReadable());

                current.writeToDisk(disk, previous);
                assertEquals(0xFE, disk.getUnsignedByte(0));
                BiomeStorage restored = BiomeStorage.readFromDisk(disk, previous);
                assertNotSame(previous, restored);
                assertEquals(id, restored.getBiome(0));
            } finally {
                network.release();
                disk.release();
            }
        }

        BiomeStorage ocean = new BiomeStorage(0);
        ocean.setBiome(1, 2);
        ByteBuf network = Unpooled.buffer();
        try {
            ocean.writeToNetwork(network, ocean.copy());
            assertNotEquals(0xFF, network.getUnsignedByte(0));
            assertEquals(2, readNetworkValues(network)[1]);
        } finally {
            network.release();
        }
    }

    @Test
    void duplicateEntriesAreNormalizedWithoutChangingValues() {
        BitArray indices = BitArrayVersion.V2.createArray(SIZE);
        indices.set(12, 1);
        indices.set(SIZE - 1, 2);
        ByteBuf disk = paletteBuffer(indices, 7, 7, 9);
        try {
            BiomeStorage storage = BiomeStorage.readFromDisk(disk, null);
            for (int index = 0; index < SIZE; index++) {
                assertEquals(index == SIZE - 1 ? 9 : 7, storage.getBiome(index));
            }

            storage.setBiome(SIZE - 1, 7);
            assertSingletonEncoding(storage, 7);
        } finally {
            disk.release();
        }
    }

    @Test
    void unusedPaletteEntriesDoNotMakeAnOtherwiseValidLayerUnreadable() {
        BitArray indices = BitArrayVersion.V16.createArray(SIZE);
        int[] palette = new int[SIZE + 1];
        for (int index = 0; index < palette.length; index++) {
            palette[index] = index;
        }

        ByteBuf disk = paletteBuffer(indices, palette);
        try {
            BiomeStorage storage = BiomeStorage.readFromDisk(disk, null);

            assertFalse(disk.isReadable());
            assertSingletonEncoding(storage, 0);
        } finally {
            disk.release();
        }
    }

    @Test
    void rejectsMalformedHeadersSizesIndicesAndTruncatedData() {
        for (int header : new int[]{1, 3, 14, 0xFE, 0xFF}) {
            ByteBuf disk = Unpooled.buffer();
            try {
                disk.writeByte(header);
                assertThrows(IllegalArgumentException.class, () -> BiomeStorage.readFromDisk(disk, null));
            } finally {
                disk.release();
            }
        }

        for (int count : new int[]{-1, 0, 3, SIZE + 1}) {
            ByteBuf disk = Unpooled.buffer();
            try {
                disk.writeByte(2);
                disk.writeZero(BitArrayVersion.V1.wordCount(SIZE) * Integer.BYTES);
                disk.writeIntLE(count);
                assertThrows(IllegalArgumentException.class, () -> BiomeStorage.readFromDisk(disk, null));
            } finally {
                disk.release();
            }
        }

        BitArray indices = BitArrayVersion.V2.createArray(SIZE);
        indices.set(10, 2);
        ByteBuf invalidIndex = paletteBuffer(indices, 1, 2);
        ByteBuf truncated = Unpooled.buffer();
        try {
            truncated.writeByte(0);
            assertThrows(IllegalArgumentException.class, () -> BiomeStorage.readFromDisk(invalidIndex, null));
            assertThrows(IndexOutOfBoundsException.class, () -> BiomeStorage.readFromDisk(truncated, null));
        } finally {
            invalidIndex.release();
            truncated.release();
        }
    }

    private static void assertSingletonEncoding(BiomeStorage storage, int expected) {
        ByteBuf disk = Unpooled.buffer();
        ByteBuf network = Unpooled.buffer();
        try {
            storage.writeToDisk(disk, null);
            assertEquals(0, disk.readUnsignedByte());
            assertEquals(expected, disk.readIntLE());
            assertFalse(disk.isReadable());

            storage.writeToNetwork(network, null);
            assertEquals(1, network.readUnsignedByte());
            assertEquals(expected, VarInts.readInt(network));
            assertFalse(network.isReadable());
        } finally {
            disk.release();
            network.release();
        }
    }

    private static int[] readNetworkValues(ByteBuf buffer) {
        BitArrayVersion version = BitArrayVersion.fromBits(buffer.readUnsignedByte() >>> 1);
        int[] words = new int[version.wordCount(SIZE)];
        for (int index = 0; index < words.length; index++) {
            words[index] = buffer.readIntLE();
        }

        int[] palette = new int[VarInts.readInt(buffer)];
        for (int index = 0; index < palette.length; index++) {
            palette[index] = VarInts.readInt(buffer);
        }

        BitArray indices = version.createArray(SIZE, words);
        int[] values = new int[SIZE];
        for (int index = 0; index < SIZE; index++) {
            values[index] = palette[indices.get(index)];
        }
        return values;
    }

    private static ByteBuf paletteBuffer(BitArray indices, int... palette) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(indices.getVersion().getBits() << 1);
        for (int word : indices.getWords()) {
            buffer.writeIntLE(word);
        }
        buffer.writeIntLE(palette.length);
        for (int biome : palette) {
            buffer.writeIntLE(biome);
        }
        return buffer;
    }
}
