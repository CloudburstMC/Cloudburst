package org.cloudburstmc.server.level.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.block.CloudBlockDefinition;
import org.cloudburstmc.server.level.chunk.bitarray.BitArray;
import org.cloudburstmc.server.level.chunk.bitarray.BitArrayVersion;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockStorageTest {

    private static final int SIZE = 4096;

    @BeforeAll
    static void initializeRegistry() {
        assertNotNull(BlockTestRegistry.get());
    }

    @Test
    void invalidWritesAndNoOpWritesLeaveTheAirSingletonIntact() {
        BlockStorage storage = new BlockStorage();

        storage.setBlock(0, BlockStates.AIR);
        assertThrows(IndexOutOfBoundsException.class, () -> storage.getBlock(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.getBlock(SIZE));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.setBlock(-1, BlockStates.STONE));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.setBlock(SIZE, BlockStates.STONE));
        assertThrows(NullPointerException.class, () -> storage.setBlock(0, null));

        assertTrue(storage.isEmpty());
        assertSame(BlockStates.AIR, storage.getBlock(0));
        assertSame(BlockStates.AIR, storage.getBlock(SIZE - 1));
        assertSingletonNetworkEncoding(storage, BlockStates.AIR);
    }

    @Test
    void everyBitWidthPreservesStatesAcrossNetworkAndDiskEncoding() {
        List<BlockState> states = BlockPalette.INSTANCE.getRuntimeMap().values().stream()
                .map(CloudBlockDefinition::getCloudState)
                .filter(state -> state != BlockStates.AIR)
                .limit(300)
                .toList();
        assertEquals(300, states.size());
        int[] sizes = {1, 2, 4, 8, 16, 32, 64, 256, 300};
        int[] widths = {1, 2, 3, 4, 5, 6, 8, 16, 16};
        BlockStorage storage = new BlockStorage();
        int populated = 0;

        for (int step = 0; step < sizes.length; step++) {
            int count = sizes[step];
            while (populated < count) {
                storage.setBlock(populated, states.get(populated));
                populated++;
            }

            ByteBuf network = Unpooled.buffer();
            ByteBuf disk = Unpooled.buffer();
            try {
                storage.writeToNetwork(network);
                assertEquals((widths[step] << 1) | 1, network.readUnsignedByte());
                BitArrayVersion version = BitArrayVersion.fromBits(widths[step]);
                int[] words = new int[version.wordCount(SIZE)];
                for (int index = 0; index < words.length; index++) {
                    words[index] = network.readIntLE();
                }

                int[] runtimeIds = new int[VarInts.readInt(network)];
                for (int index = 0; index < runtimeIds.length; index++) {
                    runtimeIds[index] = VarInts.readInt(network);
                }

                assertEquals(count + 1, runtimeIds.length);
                assertFalse(network.isReadable());
                BitArray indices = version.createArray(SIZE, words);
                storage.writeToStorage(disk);
                assertEquals(widths[step] << 1, disk.getUnsignedByte(0));
                BlockStorage restored = BlockStorage.readFromStorage(disk);
                assertFalse(disk.isReadable());

                for (int index = 0; index < SIZE; index++) {
                    BlockState expected = index < count ? states.get(index) : BlockStates.AIR;
                    assertSame(expected, restored.getBlock(index));
                    assertEquals(BlockPalette.INSTANCE.getDefinition(expected).getRuntimeId(), runtimeIds[indices.get(index)]);
                }
            } finally {
                network.release();
                disk.release();
            }
        }
    }

    @Test
    void nonAirSingletonCanBeEditedAndCopiedWithoutLosingItsFirstEntry() throws IOException {
        ByteBuf disk = paletteBuffer(null, BlockStateCodec.write(BlockStates.STONE));
        try {
            disk.writeIntLE(123456);
            BlockStorage storage = BlockStorage.readFromStorage(disk);
            assertEquals(123456, disk.readIntLE());
            assertFalse(storage.isEmpty());
            assertSingletonNetworkEncoding(storage, BlockStates.STONE);

            storage.setBlock(0, BlockStates.AIR);
            BlockStorage copy = storage.copy();
            storage.setBlock(1, BlockStates.AIR);
            copy.setBlock(SIZE - 1, BlockTypes.DIRT.getDefaultState());

            BlockStorage restored = roundTrip(copy);
            assertSame(BlockStates.AIR, restored.getBlock(0));
            assertSame(BlockStates.STONE, restored.getBlock(1));
            assertSame(BlockTypes.DIRT.getDefaultState(), restored.getBlock(SIZE - 1));
            assertSame(BlockStates.STONE, storage.getBlock(SIZE - 1));
        } finally {
            disk.release();
        }
    }

    @Test
    void duplicatePaletteEntriesCollapseWithoutAssumingEntryZeroIsAir() throws IOException {
        BitArray indices = BitArrayVersion.V2.createArray(SIZE);
        indices.set(12, 1);
        indices.set(SIZE - 1, 2);
        NbtMap stone = BlockStateCodec.write(BlockStates.STONE);
        ByteBuf disk = paletteBuffer(indices, stone, stone, BlockStateCodec.write(BlockStates.AIR));
        try {
            BlockStorage storage = roundTrip(BlockStorage.readFromStorage(disk));
            for (int index = 0; index < SIZE; index++) {
                assertSame(index == SIZE - 1 ? BlockStates.AIR : BlockStates.STONE, storage.getBlock(index));
            }
        } finally {
            disk.release();
        }
    }

    @Test
    void removingTheLastBlockCompactsBackToASingleton() {
        BlockStorage storage = new BlockStorage();
        storage.setBlock(0, BlockStates.STONE);
        storage = roundTrip(storage);
        storage.setBlock(0, BlockStates.AIR);

        BlockStorage restored = roundTrip(storage);

        assertTrue(restored.isEmpty());
        assertSingletonNetworkEncoding(restored, BlockStates.AIR);
    }

    @Test
    void rejectsRuntimeHeadersUnsupportedWidthsAndInvalidPaletteSizes() {
        for (int header : new int[]{1, 3, 14, 254, 255}) {
            ByteBuf disk = Unpooled.buffer();
            try {
                disk.writeByte(header);
                assertThrows(IllegalArgumentException.class, () -> BlockStorage.readFromStorage(disk));
            } finally {
                disk.release();
            }
        }

        for (int size : new int[]{-1, 0, 3, SIZE + 1}) {
            ByteBuf disk = Unpooled.buffer();
            try {
                disk.writeByte(2);
                disk.writeZero(BitArrayVersion.V1.wordCount(SIZE) * Integer.BYTES);
                disk.writeIntLE(size);
                assertThrows(IllegalArgumentException.class, () -> BlockStorage.readFromStorage(disk));
            } finally {
                disk.release();
            }
        }
    }

    @Test
    void rejectsOutOfRangeIndicesAndUnknownStatesInsteadOfSubstitutingAir() throws IOException {
        BitArray indices = BitArrayVersion.V2.createArray(SIZE);
        indices.set(20, 2);
        ByteBuf invalidIndex = paletteBuffer(indices,
                BlockStateCodec.write(BlockStates.STONE), BlockStateCodec.write(BlockStates.AIR));
        ByteBuf unknownState = paletteBuffer(null, NbtMap.builder()
                .putString("name", "cloudburst:unknown_storage_test")
                .putCompound("states", NbtMap.EMPTY)
                .build());
        try {
            assertThrows(IllegalArgumentException.class, () -> BlockStorage.readFromStorage(invalidIndex));
            assertThrows(IllegalArgumentException.class, () -> BlockStorage.readFromStorage(unknownState));
        } finally {
            invalidIndex.release();
            unknownState.release();
        }
    }

    private static BlockStorage roundTrip(BlockStorage storage) {
        ByteBuf disk = Unpooled.buffer();
        try {
            storage.writeToStorage(disk);
            BlockStorage restored = BlockStorage.readFromStorage(disk);
            assertFalse(disk.isReadable());
            return restored;
        } finally {
            disk.release();
        }
    }

    private static void assertSingletonNetworkEncoding(BlockStorage storage, BlockState expected) {
        ByteBuf network = Unpooled.buffer();
        try {
            storage.writeToNetwork(network);
            assertEquals(1, network.readUnsignedByte());
            assertEquals(BlockPalette.INSTANCE.getDefinition(expected).getRuntimeId(), VarInts.readInt(network));
            assertFalse(network.isReadable());
        } finally {
            network.release();
        }
    }

    private static ByteBuf paletteBuffer(BitArray indices, NbtMap... palette) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(indices == null ? 0 : indices.getVersion().getBits() << 1);
        if (indices != null) {
            for (int word : indices.getWords()) {
                buffer.writeIntLE(word);
            }
            buffer.writeIntLE(palette.length);
        }

        try (NBTOutputStream output = NbtUtils.createWriterLE(new ByteBufOutputStream(buffer))) {
            for (NbtMap state : palette) {
                output.writeTag(state);
            }
        }

        return buffer;
    }
}
